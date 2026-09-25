# layer-usage.ps1 -- dependency census for the createoreexpansion module split.
#
# Answers three questions the P3 file moves need:
#   1. Which files are referenced from more than one layer?  (candidates for `core`)
#   2. Which SHARED files reference nothing layer-specific?   (layer-clean)
#   3. P3l: is this file's WHOLE project-class closure already core-resident?
#      Reported by the `clo=D..T..` column and the `core-closure` section.
#      Being layer-clean (2) is NOT sufficient: `core` compiles against nothing in
#      src/main/java, so ONE root-resident class in the closure is enough to fail
#      :core:compileJava.  P3k learned that the hard way (15 files -> 101 errors).
#
# Read-only.  Writes build\patch\layer-usage.txt, core-candidates.txt,
# core-candidates-strict.txt, core-packages.txt, package-usage.txt and core-closure.txt.
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
    # P3r: the old skill framework moved out of foundation/** into the top-level `skill`
    # package (COE-owned), and the COE/CEWS renderer + JEI + mixin subtrees were split out.
    if ($r -match '^skill/')               { return 'COE' }
    if ($r -match '^integration/skiller/') { return 'COE' }
    if ($r -match '^client/tool/')         { return 'COE' }
    if ($r -match '^client/renderer/GrinderRenderer')     { return 'COE' }
    if ($r -match '^client/renderer/EmptyEntityRenderer') { return 'COE' }
    if ($r -match '^compat/jei/coe/')      { return 'COE' }
    if ($r -match '^compat/createaddition/') { return 'COE' }
    if ($r -match '^mixin/renderers/coe/') { return 'COE' }
    if ($r -match '^client/renderer/cews/') { return 'CEWS' }
    if ($r -match '^compat/jei/cews/')      { return 'CEWS' }
    # P3t: the THIRD sibling of the two rules above.  P3r added the COE and CEWS JEI
    # subtrees but forgot TRANS, so compat/jei/transmutation/TransmutingCategory was
    # judged SHARED while TransmutationJeiCategories (TRANS) imported it.  Same shape,
    # same reason: that package holds TRANS's JEI category renderer and nothing else.
    if ($r -match '^compat/jei/transmutation/') { return 'TRANS' }
    if ($r -notmatch '/') { return 'SHARED' }
    if ($r -match '^(common|util|foundation|compat|client|data|mixin|integration)/') { return 'SHARED' }
    return 'COE'
}

$files = Get-ChildItem -Recurse -Path $pkgRoot -Filter *.java | Sort-Object FullName

# ---------------------------------------------------------------------------
# P3l: the library tree is a SECOND source root, and `core` compiles against
# nothing in src/main/java.  So a file may only move into core when every
# project class it reaches -- directly or transitively, and through
# same-package simple names as well as imports/FQNs -- already lives under
# core/src/main/java.  "layer-clean" (the PURE mark below) is NOT enough:
# P3k moved 15 files that were layer-clean and got 101 compile errors, because
# core could not see common.energy / data.lang / foundation.item.skill / TRANS.
# The `clo=D..T..` column + core-closure.txt report exactly that closure.
# ---------------------------------------------------------------------------
$corePkgRoot = Join-Path $Repo 'core\src\main\java\com\hjmmd_8\createoreexpansion'
$coreRels = New-Object System.Collections.Generic.List[string]
if (Test-Path $corePkgRoot) {
    foreach ($f in (Get-ChildItem -Recurse -Path $corePkgRoot -Filter *.java | Sort-Object FullName)) {
        $coreRels.Add('core:' + $f.FullName.Substring($corePkgRoot.Length + 1))
    }
}
$coreRelSet = New-Object System.Collections.Generic.HashSet[string]
foreach ($cr in $coreRels) { [void]$coreRelSet.Add($cr) }

$fqnToRel = @{}
$rels = New-Object System.Collections.Generic.List[string]
foreach ($f in $files) {
    $rel = $f.FullName.Substring($pkgRoot.Length + 1)
    $rels.Add($rel)
    $fqn = $prefix + (($rel -replace '\\', '.') -replace '\.java$', '')
    $fqnToRel[$fqn] = $rel
}
# core classes must resolve too, otherwise a root file's dependency set looks
# empty the moment its target has already been moved into the library.
foreach ($cr in $coreRels) {
    $inner = $cr.Substring(5)
    $fqn = $prefix + (($inner -replace '\\', '.') -replace '\.java$', '')
    $fqnToRel[$fqn] = $cr
}

# every file of both roots; the "core:" prefix marks the library tree
$allRels = New-Object System.Collections.Generic.List[string]
foreach ($rel in $rels) { $allRels.Add($rel) }
foreach ($cr in $coreRels) { $allRels.Add($cr) }

# nested classes / static imports: `import a.b.Foo.Bar;` and `Foo.Bar.BAZ` only
# name Foo as a class -- trim trailing segments until a known class FQN is hit.
function Get-DepTargets {
    param([string]$Fqn)
    $out = New-Object System.Collections.Generic.List[string]
    $cur = $Fqn
    while ($true) {
        if ($fqnToRel.ContainsKey($cur)) { $out.Add($fqnToRel[$cur]); break }
        $i = $cur.LastIndexOf('.')
        if ($i -lt 0) { break }
        $cur = $cur.Substring(0, $i)
    }
    return ,$out
}

# simple class name -> rel, per package directory (same-package refs need no
# import and were invisible to every earlier census; one such hidden edge,
# common/AllModPotions -> common/AllModEffects, is exactly why a "clean" file
# could not move into core).
$dirSimple = @{}
foreach ($rel in $allRels) {
    $dir = Split-Path -Parent $rel
    if ([string]::IsNullOrEmpty($dir)) { $dir = '.' }
    if (-not $dirSimple.ContainsKey($dir)) { $dirSimple[$dir] = @{} }
    $dirSimple[$dir][[System.IO.Path]::GetFileNameWithoutExtension($rel)] = $rel
}
$dirRegex = @{}
foreach ($dir in $dirSimple.Keys) {
    $names = @($dirSimple[$dir].Keys | Sort-Object | ForEach-Object { [regex]::Escape($_) })
    $dirRegex[$dir] = [regex]::new('(?<![\w])(' + ($names -join '|') + ')(?![\w])')
}

# deps: source rel -> set of target rel
#   (imports + fully-qualified occurrences + same-package simple names + wildcard imports)
$deps = @{}
foreach ($rel in $allRels) { $deps[$rel] = New-Object System.Collections.Generic.HashSet[string] }

foreach ($rel in $allRels) {
    if ($rel.StartsWith('core:')) { $path = Join-Path $corePkgRoot $rel.Substring(5) }
    else                          { $path = Join-Path $pkgRoot $rel }
    $dir = Split-Path -Parent $rel
    if ([string]::IsNullOrEmpty($dir)) { $dir = '.' }
    $own = [System.IO.Path]::GetFileNameWithoutExtension($rel)
    foreach ($line in [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)) {
        $trim = $line.TrimStart()
        if ($trim -match '^(\*|//|/\*)') { continue }
        foreach ($m in [regex]::Matches($line, 'com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+')) {
            foreach ($t in (Get-DepTargets $m.Value)) {
                if ($t -ne $rel) { [void]$deps[$rel].Add($t) }
            }
        }
        # wildcard import (`import ...common.*;`): depends on EVERY class of that package.
        # Seven files in this tree use one, and a wildcard used to contribute no edge at
        # all -- the same class of blind spot as an unresolved same-package simple name.
        $wm = [regex]::Match($line, '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)\.\*;')
        if ($wm.Success) {
            $pkgDir = $wm.Groups[2].Value.Substring($prefix.Length) -replace '\.', '\'
            if ($dirSimple.ContainsKey($pkgDir)) {
                foreach ($k in $dirSimple[$pkgDir].Keys) {
                    $t = $dirSimple[$pkgDir][$k]
                    if ($t -ne $rel) { [void]$deps[$rel].Add($t) }
                }
            }
        }
        foreach ($m in $dirRegex[$dir].Matches($line)) {
            $t = $dirSimple[$dir][$m.Value]
            if ($t -ne $rel) { [void]$deps[$rel].Add($t) }
        }
    }
}

# reverse map
$refBy = @{}
foreach ($rel in $allRels) { $refBy[$rel] = New-Object System.Collections.Generic.List[string] }
foreach ($rel in $allRels) {
    foreach ($t in $deps[$rel]) { $refBy[$t].Add($rel) }
}

$layerStats = @{ 'COE' = 0; 'CEWS' = 0; 'TRANS' = 0; 'SHARED' = 0; 'CORE' = 0 }
$layerOf = @{}
foreach ($rel in $rels) {
    $l = Get-FileLayer $rel
    $layerOf[$rel] = $l
    $layerStats[$l]++
}
# files that already live under core/src/main/java are the bottom layer.  They
# are never a taint source (a root file may depend on core freely), but every
# edge into them must be recognisable so the reports do not show a blank layer.
foreach ($cr in $coreRels) { $layerOf[$cr] = 'CORE'; $layerStats['CORE']++ }

# ---- P3l: core-residency closure -------------------------------------------
# Layer-clean is necessary, not sufficient.  `core` cannot see ONE root class,
# so the test is: is every project class this file reaches already under
# core/src/main/java?  D = direct offenders, T = offenders in the whole closure.
function Get-Reach {
    param([string]$Start)
    $seen = New-Object System.Collections.Generic.HashSet[string]
    $q    = New-Object System.Collections.Generic.Queue[string]
    if ($deps.ContainsKey($Start)) {
        foreach ($t in $deps[$Start]) { if ($seen.Add($t)) { $q.Enqueue($t) } }
    }
    while ($q.Count -gt 0) {
        $n = $q.Dequeue()
        if (-not $deps.ContainsKey($n)) { continue }
        foreach ($t in $deps[$n]) { if ($seen.Add($t)) { $q.Enqueue($t) } }
    }
    return ,$seen
}

$clo = @{}
foreach ($rel in $rels) {
    $reach  = Get-Reach $rel
    $open   = New-Object System.Collections.Generic.List[string]
    $direct = New-Object System.Collections.Generic.List[string]
    foreach ($t in $reach) { if (-not $coreRelSet.Contains($t)) { $open.Add($t) } }
    foreach ($t in $deps[$rel]) { if (-not $coreRelSet.Contains($t)) { $direct.Add($t) } }
    $clo[$rel] = [pscustomobject]@{
        Open   = @($open   | Sort-Object)
        Direct = @($direct | Sort-Object)
    }
}

# ---------------------------------------------------------------------------
# P3q: layer-residency closure -- "can this layer become its own Gradle module?"
#
# check-layering.ps1 asserts DIRECTION (CEWS->COE and TRANS->COE allowed, the other
# directions forbidden).  It does NOT answer the question the P3g failure raised: a
# layer sub-module compiles against project(':core') and NOTHING in src/main/java, so
# every project class its files reach -- transitively -- must be either
#   * CORE  (already in the shared library), or
#   * the SAME layer (it travels with the sub-module), or
#   * an ALLOWED lower layer (CEWS->COE, TRANS->COE).
# ONE root-resident SHARED class (common/hub/**, data/**, CreateOreExpansion, ...) in
# that closure is enough for LAYER-NO: the sub-module cannot see it.  This column and
# build\patch\layer-closure.txt are the recomputable criterion for "can this layer be
# split out yet"; the LAYER-NO list is the work order for the next round.
# ---------------------------------------------------------------------------
$layerOrder = @('COE', 'CEWS', 'TRANS')
$layerAllowed = @{
    'COE'   = @('COE', 'CORE')
    'CEWS'  = @('CEWS', 'COE', 'CORE')
    'TRANS' = @('TRANS', 'COE', 'CORE')
}
$layerVerdict  = @{}
$layerHits     = @{}
$layerDirectNo = @{}

# BFS from a layer file; STOP at the first disallowed class on each branch and report
# it together with the first hop that led there -- that pair explains WHY the file is
# blocked.  P3s: it is NOT the edit target.  "DIRECT" now means "this file's own import
# set contains the blocker" (see the $importers index in the layer-closure section);
# the BFS hop is only printed as the "via" chain for files with no direct edge.
function Get-LayerBlockers {
    param([string]$Start, [string[]]$Allow)
    $seen     = New-Object System.Collections.Generic.HashSet[string]
    $q        = New-Object System.Collections.Generic.Queue[string]
    $firstHop = @{}
    $hits     = New-Object System.Collections.Generic.List[object]
    if ($deps.ContainsKey($Start)) {
        foreach ($t in $deps[$Start]) {
            if ($seen.Add($t)) { $q.Enqueue($t); $firstHop[$t] = $t }
        }
    }
    while ($q.Count -gt 0) {
        $n = $q.Dequeue()
        if ($Allow -notcontains $layerOf[$n]) {
            $hits.Add([pscustomobject]@{ First = $firstHop[$n]; Block = $n })
            continue
        }
        if (-not $deps.ContainsKey($n)) { continue }
        foreach ($t in $deps[$n]) {
            if ($seen.Add($t)) { $q.Enqueue($t); $firstHop[$t] = $firstHop[$n] }
        }
    }
    foreach ($h in $hits) { Write-Output $h }
}

foreach ($rel in $rels) {
    $own = $layerOf[$rel]
    if ($own -eq 'SHARED') { continue }
    $allow = $layerAllowed[$own]
    $directNo = New-Object System.Collections.Generic.List[string]
    foreach ($t in $deps[$rel]) {
        if ($allow -notcontains $layerOf[$t]) { $directNo.Add($t) }
    }
    $layerDirectNo[$rel] = @($directNo | Sort-Object)
    $layerHits[$rel]     = @(Get-LayerBlockers $rel $allow)
    if ($layerHits[$rel].Count -eq 0) { $layerVerdict[$rel] = 'LAYER-OK' }
    else                              { $layerVerdict[$rel] = 'LAYER-NO' }
}

# cross-check: the closure definition and the BFS must agree (they are two spellings
# of "is any disallowed class reachable").  A mismatch would mean a BFS bug.
$layerMismatch = 0
foreach ($rel in $rels) {
    if ($layerOf[$rel] -eq 'SHARED') { continue }
    $allow = $layerAllowed[$layerOf[$rel]]
    $badClo = 0
    foreach ($t in $clo[$rel].Open) { if ($allow -notcontains $layerOf[$t]) { $badClo++ } }
    $badBfs = @($layerHits[$rel]).Count
    if (($badClo -eq 0) -ne ($badBfs -eq 0)) {
        $layerMismatch++
        Write-Host ("layer verdict mismatch: {0} closure={1} bfs={2}" -f $rel, $badClo, $badBfs)
    }
}

$report = New-Object System.Collections.Generic.List[string]
$coreCandidates = New-Object System.Collections.Generic.List[string]

$report.Add('=== files by layer ===')
foreach ($k in @('COE', 'CEWS', 'TRANS', 'SHARED', 'CORE')) { $report.Add(("  {0,-7} {1}" -f $k, $layerStats[$k])) }
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
$report.Add('  col2 = layers this file references (CORE = target already lives in the library)')
$report.Add('  PURE = layer-clean: nothing layer-specific is reachable (necessary, NOT sufficient)')
$report.Add('  clo  = direct/transitive project deps that are NOT core-resident (P3l)')
$report.Add('  CORE-OK = layer-clean AND clo T=0  -> this file can move into core/ today')
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

    $cloCol  = ('D{0}/T{1}' -f $clo[$rel].Direct.Count, $clo[$rel].Open.Count)
    $verdict = 'CORE-NO'
    if ($clo[$rel].Open.Count -eq 0 -and -not $tainted.Contains($rel)) { $verdict = 'CORE-OK' }

    $report.Add(("  {0,-12} {1,-12} {2,4} {3,-5} clo={4,-7} {5,-8} {6}" -f `
        $u, $o, $refBy[$rel].Count, $mark, $cloCol, $verdict, $rel))
}

$report.Add('')
$report.Add('=== layer files ===')
$report.Add('  LAYER-OK = the whole project closure stays inside core + this layer + the')
$report.Add('             allowed lower layers (CEWS->COE, TRANS->COE) -> splittable today')
$report.Add('  LAYER-NO = a root-resident class is reachable -> this file cannot leave the')
$report.Add('             root project yet (blocking chain in layer-closure.txt)')
$report.Add('')
foreach ($rel in $rels) {
    if ($layerOf[$rel] -eq 'SHARED') { continue }
    $report.Add(("  {0,-7} {1,-9} {2}" -f $layerOf[$rel], $layerVerdict[$rel], $rel))
}

# ---------------------------------------------------------------------------
# P3l: the core-residency closure, per root file, with the offenders named.
# This is the answer to "can I move this file into core/ right now?".
# ---------------------------------------------------------------------------
$cloLines = New-Object System.Collections.Generic.List[string]
$okCount = 0
$noCount = 0
foreach ($rel in $rels) {
    $d = $clo[$rel].Direct.Count
    $t = $clo[$rel].Open.Count
    if ($t -eq 0) { $okCount++ } else { $noCount++ }
    $verdict = 'NO '
    if ($t -eq 0) { $verdict = 'YES' }
    $blocked = ''
    if ($t -gt 0 -and -not $tainted.Contains($rel)) { $blocked = ' (layer-clean, but reaches non-core SHARED)' }
    if ($t -gt 0 -and $tainted.Contains($rel))       { $blocked = ' (also reaches a layer)' }
    $cloLines.Add(("  {0} D={1,-3} T={2,-3} {3}{4}" -f $verdict, $d, $t, $rel, $blocked))
    if ($t -eq 0) { continue }
    $shown = 0
    foreach ($x in $clo[$rel].Direct) {
        $cloLines.Add(("        == direct      {0}" -f $x))
        $shown++
        if ($shown -ge 15) { $cloLines.Add('        == ... more direct offenders (see the file section below)'); break }
    }
    $shown = 0
    foreach ($x in $clo[$rel].Open) {
        if (@($clo[$rel].Direct) -contains $x) { continue }
        $cloLines.Add(("        -- transitive  {0}" -f $x))
        $shown++
        if ($shown -ge 15) { break }
    }
}
$cloIn = New-Object System.Collections.Generic.List[string]
$cloIn.Add('=== core-closure (P3l) ===')
$cloIn.Add('  A root file may move into core/ only when EVERY project class it reaches')
$cloIn.Add('  (import, fully-qualified reference, same-package simple name -- transitively)')
$cloIn.Add('  already lives under core/src/main/java.  D = such DIRECT offenders,')
$cloIn.Add('  T = offenders over the whole closure.  T=0 => CORE-OK.')
$cloIn.Add('')
foreach ($l in $cloLines) { $cloIn.Add($l) }
$report.Add('')
foreach ($l in $cloIn) { $report.Add($l) }

# ---------------------------------------------------------------------------
# P3q: layer-closure report -- the "can this layer be split out" work order.
# For every LAYER-NO file it prints the blocking chain: the DIRECT offender when
# there is one, otherwise "via <first hop> -> <blocker>" (the blocker sits behind an
# allowed-layer file that is itself blocked).  Then the distinct blockers per layer,
# which is the list of root-resident classes the next round has to remove.
# ---------------------------------------------------------------------------
$layerLines = New-Object System.Collections.Generic.List[string]
$layerLines.Add('=== layer-closure (P3q) ===')
$layerLines.Add('  LAYER-OK = EVERY project class this layer file reaches (import, fully-qualified')
$layerLines.Add('  reference, same-package simple name, wildcard import -- transitively) is CORE,')
$layerLines.Add('  a file of the SAME layer, or a file of an ALLOWED lower layer (CEWS->COE,')
$layerLines.Add('  TRANS->COE).  LAYER-NO = a root-resident SHARED class (common/hub/**, data/**,')
$layerLines.Add('  CreateOreExpansion, ...) is reachable: a layer sub-module compiles against')
$layerLines.Add('  project(:core) only and cannot see it.  ONE such class is enough.')
$layerLines.Add('  The split target is exactly: closure(LAYER) subset of {CORE} + allowed layers.')
$layerLines.Add('')

$layerConsole = New-Object System.Collections.Generic.List[string]
foreach ($L in $layerOrder) {
    $all = @($rels | Where-Object { $layerOf[$_] -eq $L })
    $no  = @($all | Where-Object { $layerVerdict[$_] -eq 'LAYER-NO' })
    $blockers = New-Object System.Collections.Generic.HashSet[string]
    $blockUse = @{}
    foreach ($rel in $no) {
        foreach ($h in @($layerHits[$rel])) {
            [void]$blockers.Add($h.Block)
            if (-not $blockUse.ContainsKey($h.Block)) { $blockUse[$h.Block] = New-Object System.Collections.Generic.HashSet[string] }
            [void]$blockUse[$h.Block].Add($rel)
        }
    }
    $rootBlockers = @($blockers | Where-Object { $layerOf[$_] -eq 'SHARED' })

    # ---------------------------------------------------------------------
    # P3s: the REAL direct importers, per blocker.
    #
    # The old "DIRECT -> X" line was the BFS FIRST HOP of Get-LayerBlockers:
    # when the first hop was itself inside the allowed set, the BFS walked on
    # and the blocker that surfaced could sit at the END of a long chain --
    # e.g. StellarWaveEntity "DIRECT -> <a pile of unrelated renderers>",
    # although the file imports nothing of the sort.  A chain tells you WHY a
    # file is blocked; it does not tell you WHICH file to edit.
    #
    # This index answers the actionable question instead: for blocker B, which
    # files have a DIRECT edge to B ($deps holds file-to-file edges, so this is
    # the same relation check-layering.ps1 judges).  Both spellings are printed:
    # the importer set is the work list, the chain stays as the explanation.
    # ---------------------------------------------------------------------
    $importers = @{}
    foreach ($b in $blockers) {
        $list = New-Object System.Collections.Generic.List[string]
        foreach ($x in $allRels) {
            if ($x -eq $b) { continue }
            if ($deps[$x].Contains($b)) { $list.Add($x) }
        }
        $importers[$b] = @($list | Sort-Object)
    }
    $directBlockerCount = @($blockers | Where-Object { $importers[$_].Count -gt 0 }).Count

    $layerLines.Add('----------------------------------------------------------------------')
    $layerLines.Add(("--- {0}: {1} files | LAYER-OK={2} | LAYER-NO={3}" -f `
        $L, $all.Count, ($all.Count - $no.Count), $no.Count))
    $layerLines.Add(("    distinct blocking classes: {0}  (root-resident SHARED: {1}; with a real importer: {2})" -f `
        $blockers.Count, $rootBlockers.Count, $directBlockerCount))
    $layerConsole.Add(("  {0,-6} files={1,-4} LAYER-OK={2,-4} LAYER-NO={3,-4} blockers={4,-4} root-side={5,-4} imported={6}" -f `
        $L, $all.Count, ($all.Count - $no.Count), $no.Count, $blockers.Count, $rootBlockers.Count, $directBlockerCount))
    $layerLines.Add('')
    if ($no.Count -gt 0) {
        # Group only the files whose blocker is NOT directly imported by them --
        # for those, the "via" chain IS the whole story.  Files with a real
        # direct edge get a one-line DIRECT entry, which is the edit target.
        $indOnly = New-Object System.Collections.Generic.List[string]
        foreach ($rel in $no) {
            if ($layerDirectNo[$rel].Count -gt 0) { continue }
            $indOnly.Add($rel)
        }
        $dirFiles = @($no | Where-Object { $layerDirectNo[$_].Count -gt 0 })

        $layerLines.Add(('  LAYER-NO files with a DIRECT edge to the blocker ({0}) -- these are the files to edit:' -f $dirFiles.Count))
        foreach ($rel in $dirFiles) {
            foreach ($b in $layerDirectNo[$rel]) {
                $layerLines.Add(("    DIRECT  {0}  ->  {1,-6} {2}" -f $rel, $layerOf[$b], $b))
            }
        }
        $layerLines.Add('')
        $layerLines.Add(('  LAYER-NO files blocked only through a chain ({0}):' -f $indOnly.Count))
        foreach ($rel in $indOnly) {
            $layerLines.Add(("    LAYER-NO  {0}" -f $rel))
            foreach ($h in (@($layerHits[$rel]) | Sort-Object Block)) {
                $layerLines.Add(("        via {0} -> {1,-6} {2}" -f $h.First, $layerOf[$h.Block], $h.Block))
            }
        }
        $layerLines.Add('')
        $layerLines.Add('  distinct blockers for this layer (root-resident SHARED first):')
        $layerLines.Add('    "files" = LAYER-NO files that reach it (chain included); "importers" =')
        $layerLines.Add('    files with a DIRECT edge to it -- THAT set, not the file count, is the cut list.')
        foreach ($b in (@($blockers | Where-Object { $layerOf[$_] -eq 'SHARED' }) | Sort-Object)) {
            $layerLines.Add(("    SHARED  {0,-4} files  {1,-4} importers  {2}" -f `
                $blockUse[$b].Count, $importers[$b].Count, $b))
            foreach ($x in $importers[$b]) {
                $layerLines.Add(("              <- {0,-6} {1}" -f $layerOf[$x], $x))
            }
        }
        foreach ($b in (@($blockers | Where-Object { $layerOf[$_] -ne 'SHARED' }) | Sort-Object)) {
            $layerLines.Add(("    {0,-6}  {1,-4} files  {2,-4} importers  {3}" -f `
                $layerOf[$b], $blockUse[$b].Count, $importers[$b].Count, $b))
            foreach ($x in $importers[$b]) {
                $layerLines.Add(("              <- {0,-6} {1}" -f $layerOf[$x], $x))
            }
        }
        $layerLines.Add('')
    }
}

$layerCloPath = Join-Path $Repo 'build\patch\layer-closure.txt'
[System.IO.File]::WriteAllLines($layerCloPath, $layerLines)

$outPath = Join-Path $Repo 'build\patch\layer-usage.txt'
[System.IO.File]::WriteAllLines($outPath, $report)

$candPath = Join-Path $Repo 'build\patch\core-candidates.txt'
[System.IO.File]::WriteAllLines($candPath, $coreCandidates)

$cloPath = Join-Path $Repo 'build\patch\core-closure.txt'
[System.IO.File]::WriteAllLines($cloPath, $cloIn)

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
$closedPackages = New-Object System.Collections.Generic.List[string]
$pkgFileCount = 0
$pkgPureCount = 0
$pkgMultiLayer = 0

$pkgReport.Add('=== packages ===')
$pkgReport.Add('  PURE = every file in the package is transitively clean (layer-clean only).')
$pkgReport.Add('  clo  = members whose closure reaches a file OUTSIDE core + this package.')
$pkgReport.Add('  CORE-PKG-OK = PURE and clo=0 -> the WHOLE package can move into core today')
$pkgReport.Add('         (JPMS forbids splitting one package across modules, so this is the')
$pkgReport.Add('         real move unit; a package whose members only reach EACH OTHER is')
$pkgReport.Add('         self-contained and may move as a unit even though no single file is).')
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
    # P3l: package-level closure.  A dependency on another member of the same
    # package is harmless (they travel together); anything else outside core is not.
    $pkgOpen = New-Object System.Collections.Generic.List[string]
    foreach ($rel in $members) {
        foreach ($x in $clo[$rel].Open) {
            if ($members -contains $x) { continue }
            $pkgOpen.Add(("{0} -> {1}" -f $rel, $x))
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
        if ($pkgOpen.Count -eq 0) {
            $mark = ' PURE CORE-PKG-OK'
            $closedPackages.Add($p)
        }
    }
    $pkgReport.Add(("  {0,-14} files={1,-3} uses={2,-14} refs={3,-14} clo={4,-3}{5}  {6}" -f `
        $p, $members.Count, $u, $o, $pkgOpen.Count, $mark, $p))
    foreach ($rel in ($members | Sort-Object)) {
        $s = ' ok  '
        if ($tainted.Contains($rel)) { $s = ' DIRTY' }
        $pkgReport.Add(("        {0} {1}" -f $s, $rel))
    }
    foreach ($x in ($pkgOpen | Sort-Object -Unique)) {
        $pkgReport.Add(("        OUTSIDE-CORE {0}" -f $x))
    }
    $pkgReport.Add('')
}

$pkgReport.Add('=== whole-pure packages (move unit for `core`) ===')
foreach ($p in $corePackages) {
    $pkgReport.Add(("  {0,-6} {1}" -f $pkgMembers[$p].Count, $p))
}

$pkgReport.Add('')
$pkgReport.Add('=== packages that can move into core TODAY (whole-pure + core-closed) ===')
foreach ($p in $closedPackages) {
    $pkgReport.Add(("  {0,-6} {1}" -f $pkgMembers[$p].Count, $p))
}

$pkgOut = Join-Path $Repo 'build\patch\package-usage.txt'
[System.IO.File]::WriteAllLines($pkgOut, $pkgReport)
$pkgListOut = Join-Path $Repo 'build\patch\core-packages.txt'
[System.IO.File]::WriteAllLines($pkgListOut, $corePackages)
$pkgClosedOut = Join-Path $Repo 'build\patch\core-packages-closed.txt'
[System.IO.File]::WriteAllLines($pkgClosedOut, $closedPackages)

Write-Host ('files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}  CORE={4}' -f $layerStats['COE'], $layerStats['CEWS'], $layerStats['TRANS'], $layerStats['SHARED'], $layerStats['CORE'])
Write-Host ("SHARED safe for `core` (transitively clean): {0}" -f $pure)
Write-Host ("SHARED reachable-from-a-layer (needs a decision): {0}" -f (($rels | Where-Object { $layerOf[$_] -eq 'SHARED' -and $tainted.Contains($_) } | Measure-Object).Count))
Write-Host ("SHARED referenced from more than one layer: {0}" -f $crossLayer)
Write-Host ("whole-pure packages (move as a unit): {0}  covering {1} files" -f $pkgPureCount, $pkgFileCount)
Write-Host ("  ... of which referenced from more than one layer: {0}" -f $pkgMultiLayer)
Write-Host ("packages that can move into core TODAY (whole-pure + core-closed): {0}" -f $closedPackages.Count)
Write-Host "report : $outPath"
Write-Host "candidates : $candPath"
Write-Host "package report : $pkgOut"
Write-Host "core packages  : $pkgListOut"
Write-Host "core-closed pkgs : $pkgClosedOut"

# ---- P3l: core-closure summary --------------------------------------------
Write-Host ''
Write-Host ("core-closure: {0} root files fully core-resident, {1} still reach a non-core file" -f $okCount, $noCount)
Write-Host "core-closure detail : $cloPath"
Write-Host ''
Write-Host 'top-level common/*.java verdicts (P3l move list):'
foreach ($rel in ($rels | Where-Object { (Split-Path -Parent $_) -eq 'common' })) {
    $name = [System.IO.Path]::GetFileName($rel)
    if ($clo[$rel].Open.Count -eq 0) {
        Write-Host ("  {0,-26} CORE-OK" -f $name)
    } else {
        $why = (@($clo[$rel].Direct) | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) }) -join ','
        Write-Host ("  {0,-26} BLOCKED by {1}" -f $name, $why)
    }
}

# ---- P3q: layer-closure summary -------------------------------------------
Write-Host ''
Write-Host ("layer-closure (P3q): can this layer become its own Gradle module?  (closure verdict mismatches: {0})" -f $layerMismatch)
foreach ($l in $layerConsole) { Write-Host $l }
Write-Host "layer-closure detail : $layerCloPath"
