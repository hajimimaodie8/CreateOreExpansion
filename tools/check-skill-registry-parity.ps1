# Skill registry parity check: the legacy id table (AllSkills) and the new kernel's
# registration list (SkillerIntegration) must describe EXACTLY the same 9 skill ids,
# and every skill family must still have a trigger.
#
# WHY THIS EXISTS
#   Two independent places enumerate the skills:
#     * AllSkills            -- decides which ids get written into the item data component
#     * SkillerIntegration   -- decides which ids the Skiller kernel can actually execute
#   If they drift, the symptom is a skill that "exists on the item" but never runs
#   (or a kernel entry nothing points at). compileJava / runData stay green either way,
#   because both sides are individually valid -- only the CROSS-CHECK fails.
#
# Pure ASCII. Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-skill-registry-parity.ps1
param([string]$Repo = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$fails = 0

function Read-Text([string]$rel) {
    $p = Join-Path $Repo $rel
    if (-not (Test-Path $p)) { throw "missing file: $rel" }
    return (Get-Content $p -Raw -Encoding UTF8)
}

$ALS  = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/AllSkills.java'
$SI   = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/integration/skiller/SkillerIntegration.java'
$MIX  = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/mixin/ServerPlayerGameModeMixin.java'
$USE  = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/content/skill/handler/UseItemHandler.java'
$HIT  = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/content/skill/handler/HurtLivingEntityHandler.java'
$BOW  = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/content/equipment/item/JadeTopazBowItem.java'

# ---- 1) ids declared by the legacy data registry -------------------------------
$alsText = Read-Text $ALS
$alsIds = [regex]::Matches($alsText, 'skill\(\s*"([a-z_]+)"\s*,\s*SkillType\.') |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique

# ---- 2) ids registered into the Skiller kernel --------------------------------
$siText = Read-Text $SI
$siIds = New-Object System.Collections.Generic.List[string]
foreach ($m in [regex]::Matches($siText, 'skillId\(\s*"([a-z_]+)"\s*\)')) { $siIds.Add($m.Groups[1].Value) }
foreach ($m in [regex]::Matches($siText, 'new\s+String\[\]\s*\{([^}]*)\}')) {
    foreach ($q in [regex]::Matches($m.Groups[1].Value, '"([a-z_]+)"')) { $siIds.Add($q.Groups[1].Value) }
}
foreach ($m in [regex]::Matches($siText, 'fromNamespaceAndPath\([^,]+,\s*"([a-z_]+)"\s*\)')) { $siIds.Add($m.Groups[1].Value) }
$siIds = $siIds | Sort-Object -Unique

Write-Output ("AllSkills ids        ({0}): {1}" -f $alsIds.Count, ($alsIds -join ', '))
Write-Output ("SkillerIntegration   ({0}): {1}" -f $siIds.Count, ($siIds -join ', '))

# ---- anti-vacuum: both sides must have found the expected population ----------
if ($alsIds.Count -ne 9) {
    Write-Output "FAIL(anti-vacuum): AllSkills yielded $($alsIds.Count) ids, expected 9 -- scan or registry broken."
    $fails++
}
if ($siIds.Count -ne 9) {
    Write-Output "FAIL(anti-vacuum): SkillerIntegration yielded $($siIds.Count) ids, expected 9 -- scan or registry broken."
    $fails++
}

# ---- 3) the two sets must be identical ---------------------------------------
$onlyLegacy = $alsIds | Where-Object { $_ -notin $siIds }
$onlyKernel = $siIds | Where-Object { $_ -notin $alsIds }
if ($onlyLegacy) {
    Write-Output "FAIL: id(s) written into items but NOT registered in the kernel (skill would never run):"
    $onlyLegacy | ForEach-Object { Write-Output "  - $_" }
    $fails++
}
if ($onlyKernel) {
    Write-Output "FAIL: id(s) registered in the kernel but NOT reachable from items:"
    $onlyKernel | ForEach-Object { Write-Output "  - $_" }
    $fails++
}

# ---- 4) every trigger family must still exist --------------------------------
$triggerChecks = @(
    @{ Name = 'EXCAVATION (mine blocks)'; File = $MIX; Pattern = 'CoeSkillTypes\.EXCAVATION'; Ids = @('fell','shatter','channel','grade') },
    @{ Name = 'HIT (attack entity)';      File = $HIT; Pattern = 'CoeSkillTypes\.HIT';         Ids = @('skin','plunder') },
    @{ Name = 'USE (right click item)';   File = $USE; Pattern = 'CoeSkillTypes\.USE';         Ids = @('hoe') },
    @{ Name = 'USE (bow release)';        File = $BOW; Pattern = 'CoeSkillTypes\.USE';         Ids = @('bow_curse','bow_disarm') }
)
foreach ($t in $triggerChecks) {
    $text = Read-Text $t.File
    if ($text -notmatch $t.Pattern) {
        Write-Output "FAIL: trigger family $($t.Name) missing '$($t.Pattern)' in $($t.File)"
        Write-Output "      skills that depend on it: $($t.Ids -join ', ')"
        $fails++
    } else {
        Write-Output "ok  trigger $($t.Name) <- $($t.File)  (covers $($t.Ids -join ', '))"
    }
}

if ($fails -gt 0) {
    Write-Output ""
    Write-Output "FAIL: $fails parity assertion(s) violated."
    exit 1
}
Write-Output ""
Write-Output "OK: AllSkills and the Skiller registration list agree on all 9 ids, and the four"
Write-Output "    trigger families (EXCAVATION / HIT / USE-item / USE-bow) are all present."
exit 0
