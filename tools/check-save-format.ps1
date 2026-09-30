# Save-format contract check for the item skill component (createoreexpansion:skills).
#
# WHY THIS EXISTS
#   The Skiller re-core replaced the whole skill kernel. The ONE thing that must never move is
#   the on-disk / on-wire representation of the skill data component, because old saves carry it:
#     component id : createoreexpansion:skills
#     payload      : List<String>, each entry = "<skillId>" + CompoundTag.toString()
#     parse side   : the '{...}' part is normalized '=' -> ':' before TagParser.parseTag
#     level key    : NBT "Level" (written by AllSkills.SkillBuilder#level/config, read by
#                    DataSkill#fromString and SkillEnergySpend#effectiveLevel)
#   A silent change here breaks every existing save, and NO other gate would notice
#   (compileJava / runData / layering / overlap / self-sufficiency all stay green).
#
# Each assertion carries a match-count floor so a broken scan goes RED instead of passing empty.
# Pure ASCII. Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-save-format.ps1
param([string]$Repo = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$fails = 0

function Assert-Hit {
    param([string]$Id, [string]$File, [string]$Pattern, [int]$Min = 1, [string]$Why)
    $path = Join-Path $Repo $File
    if (-not (Test-Path $path)) { Write-Output "FAIL[$Id]: file missing: $File"; $script:fails++; return }
    $n = @(Select-String -Path $path -Pattern $Pattern -Encoding UTF8 -ErrorAction SilentlyContinue).Count
    if ($n -lt $Min) {
        Write-Output "FAIL[$Id]: expected >= $Min match(es) of '$Pattern' in $File, found $n"
        Write-Output "           why it matters: $Why"
        $script:fails++
    } else {
        Write-Output "ok  [$Id] $File  (matches=$n)"
    }
}

$ADC = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/AllDataComponents.java'
$DSC = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/foundation/item/skill/DataSkill.java'
$SCC = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/foundation/item/skill/SkillsComponent.java'
$ALS = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/AllSkills.java'
$SES = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/foundation/item/skill/SkillEnergySpend.java'

# 1) component id must stay "skills" (namespace comes from the shared registerer)
Assert-Hit 'component-id' $ADC 'register\("skills"' 1 'the data component id is part of every save'

# 2) payload is a List<String> codec
Assert-Hit 'payload-codec' $ADC 'Codec<List<String>>\s+LIST_STRING_CODEC\s*=\s*Codec\.STRING\.listOf\(\)' 1 'payload type defines the on-disk shape'

# 3) serialization goes through DataSkill#toString / #fromString
Assert-Hit 'to-string' $SCC '\.map\(DataSkill::toString\)' 1 'write path of every skill entry'
Assert-Hit 'from-string' $SCC '\.map\(DataSkill::fromString\)' 1 'read path of every skill entry'

# 4) parse normalization must stay '=' -> ':' plus TagParser
Assert-Hit 'parse-brace' $DSC "indexOf\('\{'\)" 1 'split point between skill id and NBT'
Assert-Hit 'parse-normalize' $DSC "replace\('=', ':'\)" 1 'CompoundTag.toString() uses = ; TagParser needs :'
Assert-Hit 'parse-tagparser' $DSC 'TagParser\.parseTag' 1 'NBT parsing entry point'

# 5) level NBT key "Level" must still be written and read
Assert-Hit 'level-write' $ALS 'putInt\("Level"' 1 'level is persisted under this key'
Assert-Hit 'level-read' $DSC 'getInt\("Level"\)' 1 'level is restored from this key'
Assert-Hit 'level-read-eff' $SES 'getInt\("Level"\)' 1 'effective level derives from the same key'

if ($fails -gt 0) {
    Write-Output ""
    Write-Output "FAIL: $fails save-format assertion(s) violated."
    Write-Output "If a change is intentional, it needs a DataFixer for old saves -- do not just update this script."
    exit 1
}
Write-Output ""
Write-Output "OK: item skill save-format contract intact (component id / List<String> payload /"
Write-Output "    '=' -> ':' normalization / NBT \"Level\")."
exit 0
