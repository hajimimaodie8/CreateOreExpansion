# check-layering.ps1 -- architecture direction assertion for createoreexpansion.
#
# Verifies that the future module split stays acyclic:
#   CEWS  (Create: Energy Wave Studies)  is allowed to depend on COE (the material line)
#   TRANS (mechanical transmutation)     is allowed to depend on COE
#   COE   must NOT depend on CEWS or TRANS      (no reverse dependency)
#   TRANS must NOT depend on CEWS
#   CEWS  must NOT depend on TRANS        (CEWS is installable without TRANS)
#   CORE  (the shared library module) must NOT depend on COE / CEWS / TRANS
# Anything may depend on SHARED; SHARED is never judged as a source.
#
# Five source roots are scanned:
#   src/main/java/com/hjmmd_8/createoreexpansion             -> COE / CEWS / TRANS / SHARED by path
#   core/src/main/java/com/hjmmd_8/createoreexpansion        -> CORE (the whole library is shared)
#   coe/src/main/java/com/hjmmd_8/createoreexpansion         -> COE / CEWS / TRANS / SHARED by path
#   transmutation/src/main/java/com/hjmmd_8/createoreexpansion -> COE / CEWS / TRANS / SHARED by path
#   cews/src/main/java/com/hjmmd_8/createoreexpansion        -> COE / CEWS / TRANS / SHARED by path
# The second root matters: `core` is a JarJar-nested library, and without it every file
# moved into `core` would silently escape this check.
# P3w adds the THIRD root for the same reason: the Gradle sub-module `:coe` now holds the
# 176 COE files plus the @Mod entry, and a tool that still scans only the two old roots
# would print COE=0 while exiting 0 (the same silent escape P3d-beta hit for core).
# P3y adds the FOURTH root for exactly the same reason: the Gradle sub-module
# `:transmutation` now holds the 16 TRANS files (plus the @Mod entry TransmutationMod),
# and a tool that still scans only the three old roots would print TRANS=0 while exiting 0.
# P3z adds the FIFTH root for the same reason, a third time: the Gradle sub-module `:cews`
# now holds the 133 CEWS files (plus the @Mod entry CewsMod), and a tool that still scanned
# only the four old roots would print CEWS=0 while exiting 0 -- the split would look perfect
# exactly because the files had become invisible.
# The root table below is the ONLY place that knows about roots; the layer rules
# (Get-FileLayer / Get-TargetLayer) are unchanged, and every module tree is scanned with
# the same package-relative paths as the root tree (so `content/transmuting/**` still lands
# in TRANS and `common/registry/transmutation/**` still lands in TRANS, by the old rules;
# the same holds for CEWS: `content/{charger,wave,machine,energyfield}/**`,
# `common/registry/cews/**`, `compat/jei/cews/**`, `client/renderer/cews/**`).
#
# Layer of a FILE is decided purely by its path (see Get-FileLayer).
# Layer of an IMPORT / fully-qualified reference is decided by its package (see Get-TargetLayer),
# with CORE membership decided by which classes actually live under core/src/main/java.
#
# P3f note: Get-TargetLayer also accepts a BARE top-level package name, and pass 2 ignores a
# file's own `package ...;` line.  Neither changes the layer of any class reference; both close
# a false positive that only appears once a SHARED-path package lives under core/
# (core\util\*.java used to report "CORE -> COE" against their own package declaration).
#
# COE also owns the skill system, so two narrow paths are judged as COE rather than SHARED:
#   integration/skiller/**   (the Skiller integration: skill entries + strategies)
#   client/tool/**           (the skill preview renderers)
# The rest of client/ and integration/ stays SHARED on purpose.
#
# P12 (2026-09-28): the P3r/P3z rules were rewritten to recognise the PRE-SPLIT package
# names, because the split had renamed packages ONLY to satisfy these path rules -- a
# package name does not decide which Gradle module a file lives in.  So the rules now name
#   foundation/item/skill/**, foundation/{IParams,ParamsPool,FrameParams}, foundation/util/**,
#   compat/jei/{category,subcategory,animation}/**, compat/jei/{CreateOreExpansionJEI,GrindingJEI},
#   compat/{optical,vintageimprovements}/**, compat/createaddition/**,
#   client/renderer/wave/**, client/SkillSettingsScreen,
#   mixin/renderers/{EntityRendererAccessor,LivingEntityRendererAccessor}
# instead of the layer-suffixed packages the split had introduced.  The layer of every
# single file is unchanged -- only the names are honest again -- so the per-layer file
# counts must come out identical to the pre-P12 run.
#
# Exit code: 0 = clean, 1 = violations found.
#
# NOTE: this file is deliberately pure ASCII.  Windows PowerShell 5.1 reads a BOM-less
# .ps1 as ANSI, which mangles non-ASCII text and can even swallow quotes mid-script.
# Whitelist reasons are therefore written in English.

param(
    [switch]$NoFqn      # skip the fully-qualified-reference pass (imports only)
)

$ErrorActionPreference = 'Stop'

$repoRoot    = Split-Path -Parent $PSScriptRoot
$pkgRoot     = Join-Path $repoRoot 'src\main\java\com\hjmmd_8\createoreexpansion'
$corePkgRoot = Join-Path $repoRoot 'core\src\main\java\com\hjmmd_8\createoreexpansion'
$coePkgRoot  = Join-Path $repoRoot 'coe\src\main\java\com\hjmmd_8\createoreexpansion'
$transPkgRoot = Join-Path $repoRoot 'transmutation\src\main\java\com\hjmmd_8\createoreexpansion'
$cewsPkgRoot  = Join-Path $repoRoot 'cews\src\main\java\com\hjmmd_8\createoreexpansion'
$prefix      = 'com.hjmmd_8.createoreexpansion.'

# ---------------------------------------------------------------------------
# SOURCE ROOTS -- the single table that says which tree contributes what.
#   Prefix ''      : rel is package-relative, so Get-FileLayer sees the ordinary
#                    COE / CEWS / TRANS / SHARED package paths.
#   Prefix 'core\' : the whole tree is the CORE library (it has no layer packages).
# `:coe` deliberately uses the package-relative form: its tree is the same package
# layout as the root tree, and the @Mod entry CreateOreExpansion.java sits in the
# package root, so it is judged SHARED exactly like every other integration file
# (P3v section 3: "the tool already says SHARED -- believe it").
# `:transmutation` (P3y) uses it for the same reason; its 16 files all sit on
# TRANS package paths, so TRANS stays 16 and no rule had to be added.
# `:cews` (P3z) likewise: its 133 files all sit on CEWS package paths
# (content/{charger,wave,machine,energyfield}/**, common/registry/cews/**,
# compat/jei/cews/**, client/renderer/cews/**), so CEWS stays 133 and no rule was added.
# ---------------------------------------------------------------------------
$roots = @(
    @{ Path = $pkgRoot;      Prefix = '' },
    @{ Path = $corePkgRoot;  Prefix = 'core\' },
    @{ Path = $coePkgRoot;   Prefix = '' },
    @{ Path = $transPkgRoot; Prefix = '' },
    @{ Path = $cewsPkgRoot;  Prefix = '' }
)

foreach ($root in $roots) {
    if (-not (Test-Path $root.Path)) { throw "package root not found: $($root.Path)" }
}

# ---------------------------------------------------------------------------
# WHITELIST -- accepted, known cross-layer dependencies.
#
# Key   : path relative to the package root, optionally with ":<line>".
# Value : one-line reason (English, ASCII).
#
# KEEP THIS SHORT.  An entry here means "this forbidden edge is on purpose";
# it does not mean the edge is good, only that it is accepted for now.
# ---------------------------------------------------------------------------
$whitelist = [ordered]@{
    # (empty -- no accepted cross-layer edge at the time of writing)
}

function Get-FileLayer {
    param([string]$rel)
    $r = $rel -replace '\\', '/'
    # everything under core/ is the shared library, regardless of package name
    if ($r -match '^core/') { return 'CORE' }
    # explicit module paths -- must be tested BEFORE the generic "common/" SHARED rule
    if ($r -match '^common/registry/coe/')            { return 'COE' }
    # ---------------------------------------------------------------------
    # W6-a (2026-09-29): TARGET classification.
    #
    # Every file is now judged by the layer it WILL belong to after the W6-c
    # move, NOT by the Gradle module that happens to hold it today.  No file
    # has moved yet, so the rules below deliberately name the CURRENT package
    # paths of future L1 files (file-granular wherever one package will end up
    # split across two layers).  Authority for every single decision:
    # build/patch/w6-restructure-PLAN.md section 1.2.
    #
    # L1 (COE) = the wave ENGINE (wave entities, the three stress chargers,
    # the energy field) + the transmutation mechanism;
    # L2 (CEWS) = the wave MACHINES (wave gates / dispersers / stellar wave
    # transmuter / energy field controller / wave query gauge).
    # The point of this change is exactly to make the tool SEE the boundary
    # that W6-c will physically move along -- see the plan's section 2.1/2.2.
    # ---------------------------------------------------------------------
    # ---- L1 (COE): wave engine + three stress chargers + energy field ----
    # content/charger/** is L1 in full (charger blocks + wave engine helpers).
    if ($r -match '^content/charger/')     { return 'COE' }
    if ($r -match '^content/wave/api/')    { return 'COE' }
    if ($r -match '^content/energyfield/') { return 'COE' }
    # L1 files that still sit on a CEWS-looking path today; W6-c gives them a
    # COE package (common/registry/coe/charger, compat/jei/charging, client/coe).
    if ($r -match '^common/registry/cews/(AllEntityTypes|CewsRecipeProvider|CewsRecipeTypes|ChargerKineticTooltip)\.java$') { return 'COE' }
    if ($r -match '^client/renderer/cews/(CreateChargerRenderer|EmptyEntityRenderer)\.java$')                             { return 'COE' }
    if ($r -match '^compat/jei/cews/(ChargingAssemblySubCategory|ChargingCategory|ChargingJEI|WaveJadePlugin)\.java$')     { return 'COE' }
    if ($r -match '^compat/jei/cews/animation/')               { return 'COE' }
    # CewsClientSetup is split by W6-c; its L1 half (the two wave-entity renderer
    # registrations) is the part that must ship with L1 (P7a: NPE without it).
    if ($r -match '^client/cews/CewsClientSetup')              { return 'COE' }
    # ---- L2 (CEWS): the machines ----------------------------------------
    if ($r -match '^common/registry/cews/')                    { return 'CEWS' }
    if ($r -match '^content/wave/(block|frame|gauge|regulation)/') { return 'CEWS' }
    if ($r -match '^content/machine/')                         { return 'CEWS' }
    if ($r -match '^client/renderer/(cews|wave)/')             { return 'CEWS' }
    if ($r -match '^client/cews/')                             { return 'CEWS' }
    if ($r -match '^compat/jei/cews/')                         { return 'CEWS' }
    if ($r -match '^compat/(optical|vintageimprovements)/')    { return 'CEWS' }
    if ($r -match '^compat/createaddition/(TeslaCoilWaveCharger|CreateAdditionTransmuterSupport)') { return 'CEWS' }
    # ---- the third layer (TRANS): unchanged by W6-a ----------------------
    if ($r -match '^common/registry/transmutation/')  { return 'TRANS' }
    if ($r -match '^content/(transmuting|transmutation)/')        { return 'TRANS' }
    # P12 (2026-09-28): the P3r/P3z rules below are rewritten to recognise the PRE-SPLIT
    # package names.  The split had renamed packages only so that these path rules would
    # classify them -- a package name does not decide which Gradle module a file lives in
    # -- and the request was for every module's package set to match the pre-split tree
    # wherever JPMS allows it.  Each new rule classifies exactly the same FILES (same
    # layer) as the rule it replaces, so the layer counts stay byte-for-byte the same.
    #
    # The old skill framework is COE, not SHARED: foundation/item/skill/**,
    # foundation/{IParams,ParamsPool,FrameParams} and the six skill helpers of
    # foundation/util are the classes that lived there before the split.
    # foundation/util cannot be one blanket rule: BarTooltipRender was in that same
    # pre-split package but is SHARED and now lives in core/util (core and coe may not
    # claim one package), so that single exception is file-granular on purpose.
    # NOTE: only these sub-paths are COE -- the rest of client/ (MachineRotateClient,
    # client/cews/WaveQueryGaugeModelRegistration, client/render/AllRenderTypes,
    # client/renderer/{Grinder,Empty}EntityRenderer) stays out of COE for the time being.
    if ($r -match '^foundation/item/skill/')                             { return 'COE' }
    if ($r -match '^foundation/(IParams|ParamsPool|FrameParams)\.java$') { return 'COE' }
    if ($r -match '^foundation/util/') {
        if ($r -match '^foundation/util/BarTooltipRender') { return 'SHARED' }
        return 'COE'
    }
    if ($r -match '^integration/skiller/') { return 'COE' }
    if ($r -match '^client/tool/')         { return 'COE' }
    # COE-owned renderer + JEI/mixin subtrees that live under SHARED-looking parents.
    if ($r -match '^client/renderer/GrinderRenderer')          { return 'COE' }
    if ($r -match '^client/renderer/EmptyEntityRenderer')      { return 'COE' }
    if ($r -match '^client/SkillSettingsScreen')               { return 'COE' }
    if ($r -match '^compat/jei/(CreateOreExpansionJEI|GrindingJEI)') { return 'COE' }
    if ($r -match '^compat/jei/(category|subcategory|animation)/')   { return 'COE' }
    # The CC&A bridge that BOTH the lightning line (COE) and the wave line (CEWS) use.
    # CreateAdditionCompat (COE, born in P3r) moved into compat/createaddition/coe/ so
    # that the two wave-side classes could keep their pre-split package name.
    if ($r -match '^compat/createaddition/(TeslaCoilWaveCharger|CreateAdditionTransmuterSupport)') { return 'CEWS' }
    if ($r -match '^compat/createaddition/')                   { return 'COE' }
    if ($r -match '^mixin/renderers/(EntityRendererAccessor|LivingEntityRendererAccessor)') { return 'COE' }
    # CEWS-owned renderer + compat subtrees.
    if ($r -match '^client/renderer/(cews|wave)/')             { return 'CEWS' }
    if ($r -match '^compat/jei/cews/')                         { return 'CEWS' }
    if ($r -match '^compat/(optical|vintageimprovements)/')    { return 'CEWS' }
    # P3t: the THIRD sibling of the two rules above.  P3r added the COE and CEWS JEI
    # subtrees but forgot TRANS, so compat/jei/transmutation/TransmutingCategory was
    # judged SHARED while TransmutationJeiCategories (TRANS) imported it.  Same shape,
    # same reason: that package holds TRANS's JEI category renderer and nothing else.
    if ($r -match '^compat/jei/transmutation/')                { return 'TRANS' }
    # SHARED: infrastructure, never judged as a source layer
    if ($r -notmatch '/') { return 'SHARED' }                       # mod root package
    if ($r -match '^(common|util|foundation|compat|client|data|mixin|integration)/') { return 'SHARED' }
    # everything else under content/ is the mineral line
    return 'COE'
}

function Get-TargetLayer {
    param([string]$fqn)
    if ($fqn -notlike "$prefix*") { return 'SHARED' }               # not ours: ignore
    # a class that physically lives in core/ is the library layer, whatever its package.
    # P12: a NESTED class reference (`...content.wave.bridge.SubLevelBridge.Hit`) must
    # resolve to the same layer as its outer class, so trim trailing segments until a
    # known core FQN is hit -- the same walk layer-usage.ps1 Get-DepTargets has always
    # used.  Without it a core class restored to a content/** path was misread as CEWS by
    # the content/ package rule and core/compat/sable/* reported a bogus CORE -> CEWS edge.
    # This can only ever REMOVE violations: CORE is a legal target for every layer, and
    # CORE -> CORE is not a forbidden direction.
    $probe = $fqn
    while ($true) {
        if ($coreFqns.Contains($probe)) { return 'CORE' }
        $i = $probe.LastIndexOf('.')
        if ($i -lt $prefix.Length) { break }
        $probe = $probe.Substring(0, $i)
    }
    $rest = $fqn.Substring($prefix.Length)
    if ($rest -match '^common\.registry\.coe\.')            { return 'COE' }
    # W6-a (2026-09-29): the SAME target classification as Get-FileLayer, spelled
    # with package names.  The two functions must be kept in step: every file that
    # changes layer above needs its matching rule here, otherwise an import is
    # still judged by the stale rule and the file-granular exceptions above would
    # be invisible to the import pass.  See Get-FileLayer for the rationale.
    if ($rest -match '^content\.charger\.')     { return 'COE' }
    if ($rest -match '^content\.wave\.api\.')   { return 'COE' }
    if ($rest -match '^content\.energyfield\.') { return 'COE' }
    if ($rest -match '^common\.registry\.cews\.(AllEntityTypes|CewsRecipeProvider|CewsRecipeTypes|ChargerKineticTooltip)(\.|$)') { return 'COE' }
    if ($rest -match '^client\.renderer\.cews\.(CreateChargerRenderer|EmptyEntityRenderer)(\.|$)') { return 'COE' }
    if ($rest -match '^compat\.jei\.cews\.(ChargingAssemblySubCategory|ChargingCategory|ChargingJEI|WaveJadePlugin)(\.|$)') { return 'COE' }
    if ($rest -match '^compat\.jei\.cews\.animation(\.|$)') { return 'COE' }
    if ($rest -match '^client\.cews\.CewsClientSetup(\.|$)') { return 'COE' }
    if ($rest -match '^common\.registry\.cews\.')           { return 'CEWS' }
    if ($rest -match '^content\.wave\.(block|frame|gauge|regulation)\.') { return 'CEWS' }
    if ($rest -match '^content\.machine\.')                 { return 'CEWS' }
    if ($rest -match '^client\.renderer\.(cews|wave)\.')    { return 'CEWS' }
    if ($rest -match '^client\.cews\.')                     { return 'CEWS' }
    if ($rest -match '^compat\.jei\.cews\.')                { return 'CEWS' }
    if ($rest -match '^common\.registry\.transmutation\.')  { return 'TRANS' }
    if ($rest -match '^content\.(transmuting|transmutation)\.')        { return 'TRANS' }
    # skill system == COE (see Get-FileLayer); only the tool/renderer sub-packages, not all of client.
    # P12: the pre-split package names are back, so these rules mirror Get-FileLayer again.
    if ($rest -match '^foundation\.item\.skill(\.|$)')                      { return 'COE' }
    if ($rest -match '^foundation\.(IParams|ParamsPool|FrameParams)(\.|$)') { return 'COE' }
    if ($rest -match '^foundation\.util(\.|$)')                             { return 'COE' }
    if ($rest -match '^integration\.skiller\.') { return 'COE' }
    if ($rest -match '^client\.tool\.')         { return 'COE' }
    # P12: package-level COE / CEWS ownership (see Get-FileLayer).
    if ($rest -match '^client\.renderer\.GrinderRenderer(\.|$)')     { return 'COE' }
    if ($rest -match '^client\.renderer\.EmptyEntityRenderer(\.|$)') { return 'COE' }
    if ($rest -match '^client\.SkillSettingsScreen(\.|$)')           { return 'COE' }
    if ($rest -match '^compat\.jei\.(CreateOreExpansionJEI|GrindingJEI)(\.|$)') { return 'COE' }
    if ($rest -match '^compat\.jei\.(category|subcategory|animation)(\.|$)')    { return 'COE' }
    if ($rest -match '^compat\.createaddition\.(TeslaCoilWaveCharger|CreateAdditionTransmuterSupport)(\.|$)') { return 'CEWS' }
    if ($rest -match '^compat\.createaddition(\.|$)')                { return 'COE' }
    if ($rest -match '^mixin\.renderers\.(EntityRendererAccessor|LivingEntityRendererAccessor)(\.|$)') { return 'COE' }
    if ($rest -match '^client\.renderer\.(cews|wave)(\.|$)')         { return 'CEWS' }
    if ($rest -match '^compat\.jei\.cews(\.|$)')                     { return 'CEWS' }
    if ($rest -match '^compat\.(optical|vintageimprovements)(\.|$)') { return 'CEWS' }
    if ($rest -match '^compat\.jei\.transmutation(\.|$)')            { return 'TRANS' }
    # P3f: tolerate the BARE package name as well as a class inside it.  A file's own
    # `package com.hjmmd_8.createoreexpansion.util;` line is scanned by pass 2 like any
    # other fully-qualified occurrence, and without the (\.|$) alternative the string
    # "...createoreexpansion.util" missed this rule and fell through to the `return 'COE'`
    # default -> core\util\*.java reported a bogus "CORE -> COE" edge (5 false positives).
    # Get-FileLayer never had the asymmetry: `util/Foo.java` matches '^util/'.
    # Only bare top-level SHARED package names are affected -- no class FQN changes layer.
    if ($rest -match '^(common|util|foundation|compat|client|data|mixin|integration)(\.|$)') { return 'SHARED' }
    return 'COE'
}

function Test-Forbidden {
    param([string]$from, [string]$to)
    if ($from -eq 'SHARED' -or $to -eq 'SHARED') { return $false }
    if ($from -eq $to) { return $false }
    # the library is the bottom layer: it must stay ignorant of every content module
    if ($from -eq 'CORE' -and ($to -eq 'COE' -or $to -eq 'CEWS' -or $to -eq 'TRANS')) { return $true }
    if ($from -eq 'COE'   -and ($to -eq 'CEWS' -or $to -eq 'TRANS')) { return $true }
    if ($from -eq 'TRANS' -and $to -eq 'CEWS') { return $true }
    # CEWS must stay installable without TRANS (TRANS requires COE, CEWS does not), so it
    # must never reach into TRANS.  This pair used to be missing from the table -- that is
    # how the WaveEnvironmentChecks -> AllFanProcessingTypes.TRANSMUTING edge stayed
    # invisible until the stricter LAYER-OK criterion in layer-usage.ps1 surfaced it.
    if ($from -eq 'CEWS'  -and $to -eq 'TRANS') { return $true }
    return $false
}

# body text with comments stripped -- used only to flag dead imports
function Get-CodeOnly {
    param([string[]]$lines)
    return (($lines | Where-Object { $_.TrimStart() -notmatch '^(\*|//|/\*)' -and $_ -notmatch '^\s*import\s' }) -join "`n")
}

# ---- collect sources from every root ---------------------------------------
# Rel uses the platform separator, plus the root's Prefix when that root owns a whole
# layer (core), so Get-FileLayer can tell them apart before looking at packages.
$entries = New-Object System.Collections.ArrayList
foreach ($root in $roots) {
    foreach ($f in (Get-ChildItem -Recurse -Path $root.Path -Filter *.java | Sort-Object FullName)) {
        [void]$entries.Add(@{ Path = $f.FullName; Rel = $root.Prefix + $f.FullName.Substring($root.Path.Length + 1) })
    }
}

# fully-qualified names of every class that physically lives under core/
$coreFqns = New-Object System.Collections.Generic.HashSet[string]
foreach ($e in $entries) {
    if ($e.Rel -notlike 'core\*') { continue }
    $inner = $e.Rel.Substring(5)
    $fqn = $prefix + (($inner -replace '\\', '.') -replace '\.java$', '')
    [void]$coreFqns.Add($fqn)
}

$violations = New-Object System.Collections.ArrayList
$stats = @{ 'COE' = 0; 'CEWS' = 0; 'TRANS' = 0; 'SHARED' = 0; 'CORE' = 0 }
$edgeStats = @{}

foreach ($e in $entries) {
    $rel = $e.Rel
    $from = Get-FileLayer $rel
    $stats[$from]++
    if ($from -eq 'SHARED') { continue }        # SHARED never judged as a source

    $lines = [System.IO.File]::ReadAllLines($e.Path, [System.Text.Encoding]::UTF8)
    $code = Get-CodeOnly $lines

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $lineNo = $i + 1
        $isImport = $false
        $targets = New-Object System.Collections.ArrayList

        # ---- pass 1: import declarations
        $m = [regex]::Match($line, '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)\s*;')
        if ($m.Success) {
            $isImport = $true
            [void]$targets.Add($m.Groups[2].Value)
        }

        # ---- pass 2: fully-qualified references in code (skip comments and imports)
        if (-not $NoFqn -and -not $isImport) {
            $trim = $line.TrimStart()
            # a `package x.y.z;` declaration DEFINES that package, it never depends on it
            # (P3f: core\util\*.java tripped a bogus "CORE -> COE" edge on their own package line)
            if ($trim -notmatch '^(\*|//|/\*)' -and $trim -notmatch '^package\s') {
                foreach ($mm in [regex]::Matches($line, 'com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+')) {
                    [void]$targets.Add($mm.Value)
                }
            }
        }

        foreach ($fqn in $targets) {
            $to = Get-TargetLayer $fqn
            if (-not (Test-Forbidden $from $to)) { continue }
            $key = "$rel`:$lineNo"
            $wlReason = $null
            if ($whitelist.Contains($key)) { $wlReason = $whitelist[$key] }
            elseif ($whitelist.Contains($rel)) { $wlReason = $whitelist[$rel] }
            if ($wlReason) { continue }

            $tag = ''
            if (-not $isImport) { $tag = ' [fully-qualified, not an import]' }
            $simple = ($fqn -split '\.')[-1]
            if ($isImport -and $code -notmatch ('(?<![\w.])' + [regex]::Escape($simple) + '(?![\w])')) {
                $tag = ' [dead import: simple name never used in code]'
            }
            $edge = "$from -> $to"
            if (-not $edgeStats.ContainsKey($edge)) { $edgeStats[$edge] = 0 }
            $edgeStats[$edge]++
            [void]$violations.Add(("{0}:{1} -> {2}{3}   ({4})" -f $rel, $lineNo, $to, $tag, $fqn))
        }
    }
}

Write-Host 'layering check: src/main/java + core/src/main/java + coe/src/main/java + transmutation/src/main/java + cews/src/main/java'
Write-Host ('  files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}  CORE(library)={4}' -f $stats['COE'], $stats['CEWS'], $stats['TRANS'], $stats['SHARED'], $stats['CORE'])
Write-Host '  forbidden edge directions checked : COE->CEWS, COE->TRANS, TRANS->CEWS, CEWS->TRANS, CORE->COE/CEWS/TRANS'
if ($NoFqn) { Write-Host '  (fully-qualified-reference pass disabled by -NoFqn)' }

if ($violations.Count -eq 0) {
    Write-Host 'OK: no forbidden cross-layer dependency found.'
    exit 0
}

Write-Host ''
Write-Host ('VIOLATIONS ({0}):' -f $violations.Count)
foreach ($v in ($violations | Sort-Object)) { Write-Host ('  ' + $v) }
Write-Host ''
Write-Host 'by direction:'
foreach ($k in ($edgeStats.Keys | Sort-Object)) { Write-Host ('  {0} : {1}' -f $k, $edgeStats[$k]) }
if ($whitelist.Count -eq 0) { Write-Host 'whitelist: (empty)' }
else { Write-Host ('whitelist entries: ' + $whitelist.Count) }
exit 1
