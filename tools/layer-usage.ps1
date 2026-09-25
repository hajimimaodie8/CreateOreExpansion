# layer-usage.ps1 -- dependency census for the createoreexpansion module split.
#
# Answers two questions the P3 file moves need:
#   1. Which files are referenced from more than one layer?  (candidates for `core`)
#   2. Which SHARED files reference nothing layer-specific?   (safe to move to `core`)
#
# Read-only.  Writes build\patch\layer-usage.txt and build\patch\core-candidates.txt.
#
# Layer rules are copied verbatim from tools\check-layering.ps1 so the two tools
# always agree.  P3e added the two COE skill paths (integration/skiller/**, client/tool/**)
# to BOTH tools; without that, COE skill files would be counted as SHARED-PURE and land
# in core-candidates.txt, which is actively wrong.
# Deliberately pure ASCII: PowerShell 5.1 reads a BOM-less .ps1 as
# ANSI and can swallow quotes mid-script.

param(
    [string]$Repo = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'

$pkgRoot = Join-Path $Repo 'src\main\java\com\hjmmd_8\createoreexpansion'
$prefix  = 'com.hjmmd_8.createoreexpansion.'
if (-not (Test-Path $pkgRoot)) { throw "package root not found: $pkgRoot" }

function Get-FileLayer {
    param([string]$rel)
    $r = $rel -replace '\\', '/'
    if ($r -match '^common/registry/coe/')            { return 'COE' }
    if ($r -match '^common/registry/cews/')           { return 'CEWS' }
    if ($r -match '^common/registry/transmutation/')  { return 'TRANS' }
    if ($r -match '^content/(charger|wave|machine|energyfield)/') { return 'CEWS' }
    if ($r -match '^content/(transmuting|transmutation)/')        { return 'TRANS' }
    # the skill system belongs to COE -- kept in sync with tools\check-layering.ps1 (P3e)
    if ($r -match '^integration/skiller/') { return 'COE' }
    if ($r -match '^client/tool/')         { return 'COE' }
    if ($r -notmatch '/') { return 'SHARED' }
    if ($r -match '^(common|util|foundation|compat|client|data|mixin|integration)/') { return 'SHARED' }
    return 'COE'
}

$files = Get-ChildItem -Recurse -Path $pkgRoot -Filter *.java | Sort-Object FullName

$fqnToRel = @{}
$rels = New-Object System.Collections.Generic.List[string]
foreach ($f in $files) {
    $rel = $f.FullName.Substring($pkgRoot.Length + 1)
    $rels.Add($rel)
    $fqn = $prefix + (($rel -replace '\\', '.') -replace '\.java$', '')
    $fqnToRel[$fqn] = $rel
}

# deps: source rel -> set of target rel (imports + fully-qualified occurrences)
$deps = @{}
foreach ($rel in $rels) { $deps[$rel] = New-Object System.Collections.Generic.HashSet[string] }

foreach ($rel in $rels) {
    $path = Join-Path $pkgRoot $rel
    foreach ($line in [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)) {
        $trim = $line.TrimStart()
        if ($trim -match '^(\*|//|/\*)') { continue }
        foreach ($m in [regex]::Matches($line, 'com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+')) {
            $fqn = $m.Value
            if ($fqnToRel.ContainsKey($fqn)) {
                $t = $fqnToRel[$fqn]
                if ($t -ne $rel) { [void]$deps[$rel].Add($t) }
            }
        }
    }
}

# reverse map
$refBy = @{}
foreach ($rel in $rels) { $refBy[$rel] = New-Object System.Collections.Generic.List[string] }
foreach ($rel in $rels) {
    foreach ($t in $deps[$rel]) { $refBy[$t].Add($rel) }
}

$layerStats = @{ 'COE' = 0; 'CEWS' = 0; 'TRANS' = 0; 'SHARED' = 0 }
$layerOf = @{}
foreach ($rel in $rels) {
    $l = Get-FileLayer $rel
    $layerOf[$rel] = $l
    $layerStats[$l]++
}

$report = New-Object System.Collections.Generic.List[string]
$coreCandidates = New-Object System.Collections.Generic.List[string]

$report.Add('=== files by layer ===')
foreach ($k in @('COE', 'CEWS', 'TRANS', 'SHARED')) { $report.Add(("  {0,-7} {1}" -f $k, $layerStats[$k])) }
$report.Add('')

# Taint propagation: a SHARED file may only move into `core` if NOTHING it depends on
# (directly or transitively, through other SHARED files) is layer-specific.  Otherwise
# `core` would have to import COE/CEWS/TRANS, which is exactly the edge we forbid.
$tainted = New-Object System.Collections.Generic.HashSet[string]
$queue   = New-Object System.Collections.Generic.Queue[string]
foreach ($rel in $rels) {
    if ($layerOf[$rel] -ne 'SHARED') { [void]$tainted.Add($rel); $queue.Enqueue($rel) }
}
while ($queue.Count -gt 0) {
    $n = $queue.Dequeue()
    foreach ($u in $refBy[$n]) {
        if ($layerOf[$u] -eq 'SHARED' -and -not $tainted.Contains($u)) {
            [void]$tainted.Add($u)
            $queue.Enqueue($u)
        }
    }
}

$report.Add('=== SHARED files ===')
$report.Add('  col1 = referencing layers (outside its own layer, excluding SHARED)')
$report.Add('  col2 = layers this file references')
$report.Add('  PURE = moves into `core` with no further work (nothing layer-specific reachable)')
$report.Add('')

$pure = 0
$crossLayer = 0

foreach ($rel in $rels) {
    if ($layerOf[$rel] -ne 'SHARED') { continue }

    $users = New-Object System.Collections.Generic.HashSet[string]
    foreach ($u in $refBy[$rel]) {
        $ul = $layerOf[$u]
        if ($ul -ne 'SHARED') { [void]$users.Add($ul) }
    }
    $out = New-Object System.Collections.Generic.HashSet[string]
    foreach ($t in $deps[$rel]) {
        $tl = $layerOf[$t]
        if ($tl -ne 'SHARED') { [void]$out.Add($tl) }
    }

    $u = (($users | Sort-Object) -join ',')
    $o = (($out | Sort-Object) -join ',')
    if ($u -eq '') { $u = '-' }
    if ($o -eq '') { $o = '-' }

    $mark = ''
    if (-not $tainted.Contains($rel)) {
        $pure++
        $mark = ' PURE'
        $coreCandidates.Add($rel)
    }
    if ($users.Count -gt 1) { $crossLayer++ }

    $report.Add(("  {0,-12} {1,-12} {2,4}{3}  {4}" -f $u, $o, $refBy[$rel].Count, $mark, $rel))
}

$report.Add('')
$report.Add('=== layer files ===')
foreach ($rel in $rels) {
    if ($layerOf[$rel] -eq 'SHARED') { continue }
    $report.Add(("  {0,-7} {1}" -f $layerOf[$rel], $rel))
}

$outPath = Join-Path $Repo 'build\patch\layer-usage.txt'
[System.IO.File]::WriteAllLines($outPath, $report)

$candPath = Join-Path $Repo 'build\patch\core-candidates.txt'
[System.IO.File]::WriteAllLines($candPath, $coreCandidates)

# `core` is the lowest module: a file there may be used by any layer, but it must not
# reach one.  Being clean is necessary, not sufficient -- a clean file that only COE
# uses belongs in COE for cohesion.  The strict list is the likely P3d core set.
$strict = New-Object System.Collections.Generic.List[string]
foreach ($rel in $coreCandidates) {
    $users = New-Object System.Collections.Generic.HashSet[string]
    foreach ($u in $refBy[$rel]) {
        if ($layerOf[$u] -ne 'SHARED') { [void]$users.Add($layerOf[$u]) }
    }
    if ($users.Count -gt 1) { $strict.Add($rel) }
}
$strictPath = Join-Path $Repo 'build\patch\core-candidates-strict.txt'
[System.IO.File]::WriteAllLines($strictPath, $strict)
Write-Host ("strict core set (clean AND used by more than one layer): {0}" -f $strict.Count)
Write-Host "strict list : $strictPath"

Write-Host ('files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}' -f $layerStats['COE'], $layerStats['CEWS'], $layerStats['TRANS'], $layerStats['SHARED'])
Write-Host ("SHARED safe for `core` (transitively clean): {0}" -f $pure)
Write-Host ("SHARED reachable-from-a-layer (needs a decision): {0}" -f (($rels | Where-Object { $layerOf[$_] -eq 'SHARED' -and $tainted.Contains($_) } | Measure-Object).Count))
Write-Host ("SHARED referenced from more than one layer: {0}" -f $crossLayer)
Write-Host "report : $outPath"
Write-Host "candidates : $candPath"
