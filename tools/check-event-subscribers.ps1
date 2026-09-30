# Event-subscriber baseline check.
#
# WHY THIS EXISTS
#   A class annotated with @EventBusSubscriber (or one carrying @SubscribeEvent methods) is
#   invoked by the event bus, NOT by other classes. A "find references by class name" audit
#   therefore reports ZERO references for it and will happily call it dead code.
#   On 2026-09-30 exactly that happened during the Skiller re-core: HurtLivingEntityHandler and
#   UseItemHandler were deleted as "dead", which silently broke the HIT-family skills
#   (skin/plunder) and the USE-family skills (hoe) -- while compileJava, runData, layering,
#   package-overlap and the 92 self-sufficiency assertions all stayed GREEN.
#
# WHAT IT CHECKS
#   * every path in tools/event-subscriber-baseline.txt must still be annotated  -> MISSING = RED
#   * newly annotated files are reported as INFO (normal development; update the baseline)
#
# Pure ASCII. Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-event-subscribers.ps1
#        [-Repo <path>] [-UpdateBaseline]
param(
    [string]$Repo = (Split-Path -Parent $PSScriptRoot),
    [switch]$UpdateBaseline
)

$ErrorActionPreference = 'Stop'
$roots = @('src\main\java', 'core\src\main\java', 'coe\src\main\java', 'cews\src\main\java', 'transmutation\src\main\java')
$baselinePath = Join-Path $PSScriptRoot 'event-subscriber-baseline.txt'

$found = @()
foreach ($r in $roots) {
    $dir = Join-Path $Repo $r
    if (-not (Test-Path $dir)) { continue }
    $files = Get-ChildItem $dir -Recurse -File -Filter *.java -ErrorAction SilentlyContinue
    foreach ($f in $files) {
        $hit = Select-String -Path $f.FullName -Pattern '@EventBusSubscriber' -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($hit) {
            $rel = $f.FullName.Replace("$Repo\", '').Replace('\', '/')
            $found += $rel
        }
    }
}
$found = $found | Sort-Object -Unique

if ($UpdateBaseline) {
    $found | Set-Content -Path $baselinePath -Encoding UTF8
    Write-Output "baseline updated: $($found.Count) entries -> $baselinePath"
    exit 0
}

if (-not (Test-Path $baselinePath)) {
    Write-Output "FAIL: baseline missing: $baselinePath"
    Write-Output "      run with -UpdateBaseline once, review the list, then commit it."
    exit 1
}

$baseline = Get-Content $baselinePath -Encoding UTF8 | Where-Object { $_.Trim() -ne '' } | ForEach-Object { $_.Trim() } | Sort-Object -Unique

# anti-vacuum guards: if either side is empty the check is meaningless, so it must go red.
$guard = 0
if ($found.Count -lt 20) { Write-Output "FAIL(anti-vacuum): scan found only $($found.Count) subscribers; expected >= 20 -- scan is broken."; $guard++ }
if ($baseline.Count -lt 20) { Write-Output "FAIL(anti-vacuum): baseline has only $($baseline.Count) entries; expected >= 20."; $guard++ }
if ($guard -gt 0) { exit 1 }

$missing = Compare-Object $baseline $found | Where-Object { $_.SideIndicator -eq '<=' } | ForEach-Object { $_.InputObject }
$added = Compare-Object $baseline $found | Where-Object { $_.SideIndicator -eq '=>' } | ForEach-Object { $_.InputObject }

Write-Output "subscribers: found=$($found.Count) baseline=$($baseline.Count)"

if ($added) {
    Write-Output "INFO: new @EventBusSubscriber file(s) not in baseline (update the baseline if intended):"
    $added | ForEach-Object { Write-Output "  + $_" }
}

if ($missing) {
    Write-Output "FAIL: @EventBusSubscriber file(s) present in baseline but no longer annotated/found:"
    $missing | ForEach-Object { Write-Output "  - $_" }
    Write-Output ""
    Write-Output "If this was intentional, delete the line from tools/event-subscriber-baseline.txt in the SAME commit"
    Write-Output "and state in the commit message which in-game trigger you are removing."
    exit 1
}

Write-Output "OK: every baselined event subscriber is still present (no entry point silently lost)."
exit 0
