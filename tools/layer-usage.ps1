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

# ---------------------------------------------------------------------------
# P3f: package-level aggregation.
#
# JPMS says one Java package may not belong to two mod files at once, so the move
# unit is a WHOLE package: a package may only enter `core` when every one of its
# files is transitively clean (i.e. -not $tainted).  A single dirty file keeps the
# whole package in the root project.
#
# Writes build\patch\core-packages.txt     -- package dirs that are whole-pure
#        build\patch\package-usage.txt     -- full per-package report
# ---------------------------------------------------------------------------

$pkgOf = @{}
foreach ($rel in $rels) {
    $dir = Split-Path -Parent $rel
    if ([string]::IsNullOrEmpty($dir)) { $dir = '.' }   # the mod root package
    $pkgOf[$rel] = $dir
}

$pkgMembers = @{}
foreach ($rel in $rels) {
    $p = $pkgOf[$rel]
    if (-not $pkgMembers.ContainsKey($p)) { $pkgMembers[$p] = New-Object System.Collections.Generic.List[string] }
    $pkgMembers[$p].Add($rel)
}

$pkgReport    = New-Object System.Collections.Generic.List[string]
$corePackages = New-Object System.Collections.Generic.List[string]
$pkgFileCount = 0
$pkgPureCount = 0
$pkgMultiLayer = 0

$pkgReport.Add('=== packages ===')
$pkgReport.Add('  PURE = every file in the package is transitively clean -> the WHOLE package')
$pkgReport.Add('         may move into `core` (JPMS forbids splitting a package across modules).')
$pkgReport.Add('  uses = layers (COE/CEWS/TRANS) that reference at least one file of this package.')
$pkgReport.Add('')

foreach ($p in ($pkgMembers.Keys | Sort-Object)) {
    $members = $pkgMembers[$p]
    $dirty   = New-Object System.Collections.Generic.List[string]
    $users   = New-Object System.Collections.Generic.HashSet[string]
    $outSet  = New-Object System.Collections.Generic.HashSet[string]
    foreach ($rel in $members) {
        if ($tainted.Contains($rel)) { $dirty.Add($rel) }
        foreach ($u in $refBy[$rel]) {
            $ul = $layerOf[$u]
            if ($ul -ne 'SHARED') { [void]$users.Add($ul) }
        }
        foreach ($t in $deps[$rel]) {
            $tl = $layerOf[$t]
            if ($tl -ne 'SHARED') { [void]$outSet.Add($tl) }
        }
    }
    $whole = ($dirty.Count -eq 0)
    $u = (($users  | Sort-Object) -join ',')
    $o = (($outSet | Sort-Object) -join ',')
    if ($u -eq '') { $u = '-' }
    if ($o -eq '') { $o = '-' }
    $mark = ''
    if ($whole) {
        $mark = ' PURE'
        $pkgPureCount++
        $pkgFileCount += $members.Count
        $corePackages.Add($p)
        if ($users.Count -gt 1) { $pkgMultiLayer++ }
    }
    $pkgReport.Add(("  {0,-14} files={1,-3} uses={2,-14} refs={3,-14}{4}  {5}" -f `
        $p, $members.Count, $u, $o, $mark, $p))
    foreach ($rel in ($members | Sort-Object)) {
        $s = ' ok  '
        if ($tainted.Contains($rel)) { $s = ' DIRTY' }
        $pkgReport.Add(("        {0} {1}" -f $s, $rel))
    }
    $pkgReport.Add('')
}

$pkgReport.Add('=== whole-pure packages (move unit for `core`) ===')
foreach ($p in $corePackages) {
    $pkgReport.Add(("  {0,-6} {1}" -f $pkgMembers[$p].Count, $p))
}

$pkgOut = Join-Path $Repo 'build\patch\package-usage.txt'
[System.IO.File]::WriteAllLines($pkgOut, $pkgReport)
$pkgListOut = Join-Path $Repo 'build\patch\core-packages.txt'
[System.IO.File]::WriteAllLines($pkgListOut, $corePackages)

Write-Host ('files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}' -f $layerStats['COE'], $layerStats['CEWS'], $layerStats['TRANS'], $layerStats['SHARED'])
Write-Host ("SHARED safe for `core` (transitively clean): {0}" -f $pure)
Write-Host ("SHARED reachable-from-a-layer (needs a decision): {0}" -f (($rels | Where-Object { $layerOf[$_] -eq 'SHARED' -and $tainted.Contains($_) } | Measure-Object).Count))
Write-Host ("SHARED referenced from more than one layer: {0}" -f $crossLayer)
Write-Host ("whole-pure packages (move as a unit): {0}  covering {1} files" -f $pkgPureCount, $pkgFileCount)
Write-Host ("  ... of which referenced from more than one layer: {0}" -f $pkgMultiLayer)
Write-Host "report : $outPath"
Write-Host "candidates : $candPath"
Write-Host "package report : $pkgOut"
Write-Host "core packages  : $pkgListOut"
