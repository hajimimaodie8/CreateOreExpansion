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
# `:transmutation` held the 16 TRANS files (plus the @Mod entry TransmutationMod),
# and a tool that still scanned only the three old roots would print TRANS=0 while exiting 0.
# W6-b2 (2026-09-29): those 16 files moved into `:coe` (package names verbatim), so the
# fourth root now holds ONE file -- the empty @Mod shell (TRANS=1 is the honest count).
# The root stays in this table for exactly that reason: dropping it would make the third
# layer invisible instead of empty.
# P3z adds the FIFTH root for the same reason, a third time: the Gradle sub-module `:cews`
# now holds the 133 CEWS files (plus the @Mod entry CewsMod), and a tool that still scanned
# only the four old roots would print CEWS=0 while exiting 0 -- the split would look perfect
# exactly because the files had become invisible.
# The root table below is the ONLY place that knows about roots; the layer rules
# (Get-FileLayer / Get-TargetLayer) are unchanged, and every module tree is scanned with
# the same package-relative paths as the root tree.  W6-b2 re-judged the transmutation
# paths (they are COE now, because that is the module they ship in), which is the same
# "target classification" W6-a introduced for the wave engine.
#
# W8-b (2026-09-30) adds the missing half of this tool: PHYSICAL MODULE vs JUDGED LAYER.
#   Everything above decides a file's layer from its PATH, so a file that lands under a
#   SHARED prefix (common/**, compat/**, client/**, data/**, util/**, integration/**) is
#   never judged as a source at all -- its outgoing edges are unconstrained, no matter
#   which Gradle module it physically ships in.  Two consequences, both silent:
#     (1) a file placed in the WRONG module still looks fine, because only its package
#         path is read (a CEWS machine dropped into coe/src/main/java becomes a coe-owned
#         file that no gate notices, and cews.jar then simply misses it);
#     (2) a SHARED-path file inside :coe may import :transmutation without ever tripping
#         the COE -> TRANS rule, because the rule was applied to "SHARED".
#   The fix is two new assertion groups, both derived, neither a whitelist:
#     M1  each module's tree may only contain layers from $moduleAllowedLayers (see the
#         table for the architecture basis of each set), and each of the five roots must
#         contribute at least one file -- the "layer count 0 and still exit 0" escape
#         this tool has hit four times (P3d-beta core / P3w coe / P3y transmutation /
#         P3z cews) is now a hard failure instead of a plausible-looking number.
#     M2  a SHARED-path file is judged by the MODULE it ships in for direction purposes:
#         :coe -> COE, :cews -> CEWS, :transmutation -> TRANS, core -> CORE, and the root
#         tree (the integration layer, which ships in no module jar) stays unconstrained.
#         Measured before landing: 22 SHARED-path files live in content modules and this
#         rule produced 0 new violations, so it is a pure strengthening.
#   SHARED / CORE handling, stated explicitly:
#     * SHARED is allowed in every content module.  It means "infrastructure that is not
#       layer-owned", and each module legitimately holds some (the @Mod entry, registries
#       for its own layer's shared plumbing).  It is NOT allowed to be an escape hatch --
#       that is what M2 closes.
#     * CORE is decided by the `^core/` prefix alone, so "core contains only CORE" is a
#       TAUTOLOGY (the table entry is documentation, not a detector).  The real core-side
#       guards stay where they were: the CORE -> COE/CEWS/TRANS direction rule above,
#       plus tools\check-package-overlap.ps1 for the JPMS package rule.
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
    @{ Module = 'root';          Path = $pkgRoot;      Prefix = '' },
    @{ Module = 'core';          Path = $corePkgRoot;  Prefix = 'core\' },
    @{ Module = 'coe';           Path = $coePkgRoot;   Prefix = '' },
    @{ Module = 'transmutation'; Path = $transPkgRoot; Prefix = '' },
    @{ Module = 'cews';          Path = $cewsPkgRoot;  Prefix = '' }
)

foreach ($root in $roots) {
    if (-not (Test-Path $root.Path)) { throw "package root not found: $($root.Path)" }
}

# ---------------------------------------------------------------------------
# W8-b: the physical-module assertion tables (see the W8-b note in the header).
#
# $moduleAllowedLayers -- the ONLY basis is the architecture, and each set is written
# out with its reason so a later reader can check the claim instead of trusting it:
#   root          = integration layer.  It is NOT a shipped artifact (the three module
#                   jars are), so a layer-owned file left here reaches no module jar --
#                   exactly the data-side hole E2/F1 already pin.  Today it holds 7
#                   dev-only files, every one of them on a SHARED path.
#   core          = the JarJar-nested shared library.  Decided by the `^core/` prefix,
#                   so this set is documentation (see header).
#   coe           = layer 1: mineral line + wave engine + transmutation mechanism + the
#                   skill system.  SHARED allowed (its @Mod entry, its own registries).
#   cews          = layer 2: wave machines.  SHARED allowed for the same reason.
#   transmutation = layer 3: the empty @Mod shell today.  SHARED allowed for the same
#                   reason.
# $moduleOwnLayer -- the layer a SHARED-path file in that module is judged as for the
# direction rules (M2).  '' means "do not constrain" (the integration layer only).
# ---------------------------------------------------------------------------
$moduleAllowedLayers = [ordered]@{
    'root'          = @('SHARED')
    'core'          = @('CORE')
    'coe'           = @('COE', 'SHARED')
    'cews'          = @('CEWS', 'SHARED')
    'transmutation' = @('TRANS', 'SHARED')
}
$moduleOwnLayer = @{
    'root'          = ''
    'core'          = 'CORE'
    'coe'           = 'COE'
    'cews'          = 'CEWS'
    'transmutation' = 'TRANS'
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
    # COE package (common/registry/coe/charger, compat/jei/*, client/renderer, client/).
    # W6-c (2026-09-29): the move is DONE, so these rules now name the NEW paths.
    # The old-path forms were deleted together with the move (a rule that matches no
    # file is the "looks like it manages something, manages nothing" shape).
    # NOTE: common/registry/coe/charger/** needs no rule of its own -- the generic
    # `^common/registry/coe/` test above already returns COE for it.
    if ($r -match '^client/renderer/CreateChargerRenderer\.java$') { return 'COE' }
    if ($r -match '^compat/jei/ChargingJEI\.java$') { return 'COE' }
    # W6-d (2026-09-30): WaveJadePlugin is a JADE plugin, not a JEI one, so it went back to
    # the pre-split package compat/jade (where its sibling BasinLiveJadePlugin already lives).
    # FILE-granular on purpose: the generic `^compat/` SHARED rule below still governs
    # compat/jade/BasinLiveJadePlugin, and only this one file is L1.  Its LAYER IS UNCHANGED
    # (COE -> COE), so every per-layer count stays byte-for-byte what it was.
    if ($r -match '^compat/jade/WaveJadePlugin\.java$') { return 'COE' }
    # W6-d: ChargingRecipeTools left `core` for :coe (plan section 2.4 P2 -- its writers
    # (CoeItems, 7 sites) and its only reader (CoeChargingRecipeProvider) are all in :coe
    # now).  PACKAGE-granular: `common/charger/` exists only in :coe, so a future file there
    # must not silently fall back to the generic `^common/` SHARED rule.  This one file IS a
    # real layer change (CORE -> COE) -- that is the point of the move, not a side effect.
    if ($r -match '^common/charger/')      { return 'COE' }
    # CewsClientSetup is split by W6-c; its L1 half (the two wave-entity renderer
    # registrations) moved into the existing :coe package `client`.
    if ($r -match '^client/WaveEntityRendererRegistration\.java$') { return 'COE' }
    # ---- L2 (CEWS): the machines ----------------------------------------
    if ($r -match '^common/registry/cews/')                    { return 'CEWS' }
    if ($r -match '^content/wave/(block|frame|gauge|regulation)/') { return 'CEWS' }
    if ($r -match '^content/machine/')                         { return 'CEWS' }
    if ($r -match '^client/renderer/(cews|wave)/')             { return 'CEWS' }
    if ($r -match '^client/cews/')                             { return 'CEWS' }
    if ($r -match '^compat/jei/cews/')                         { return 'CEWS' }
    if ($r -match '^compat/(optical|vintageimprovements)/')    { return 'CEWS' }
    if ($r -match '^compat/createaddition/(TeslaCoilWaveCharger|CreateAdditionTransmuterSupport)') { return 'CEWS' }
    # ---- the third layer (TRANS): W6-b2 -----------------------------------
    # W6-b2 (2026-09-29): the transmutation MECHANISM moved into :coe, so every
    # file that used to be judged TRANS by its package path is COE now -- the
    # same "target classification" W6-a started (the file and the layer it
    # ships in must agree again).
    # What is left of the third layer is exactly ONE file: its empty @Mod shell,
    # which had to move to a package of its own
    # (com.hjmmd_8.createoreexpansion.transmutation) because
    # common.registry.transmutation is claimed by :coe now and two mod files may
    # never declare one package (JPMS ResolutionException).
    if ($r -match '^transmutation/TransmutationMod\.java$')           { return 'TRANS' }
    if ($r -match '^common/registry/transmutation/')  { return 'COE' }
    if ($r -match '^content/(transmuting|transmutation)/')        { return 'COE' }
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
    # judged SHARED while TransmutationJeiCategories imported it.
    # W6-b2: that whole subtree is COE now (the transmutation mechanism moved into :coe).
    if ($r -match '^compat/jei/transmutation/')                { return 'COE' }
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
    # W6-c (2026-09-29): the move is DONE; these rules name the NEW package names of
    # the files that changed package (the old-path forms are gone).  Keep in step with
    # Get-FileLayer above -- that is the whole point of the pair.
    if ($rest -match '^common\.registry\.coe\.charger(\.|$)') { return 'COE' }
    if ($rest -match '^client\.renderer\.CreateChargerRenderer(\.|$)') { return 'COE' }
    if ($rest -match '^compat\.jei\.ChargingJEI(\.|$)') { return 'COE' }
    # W6-d: keep in step with Get-FileLayer above (that is the whole point of the pair).
    if ($rest -match '^compat\.jade\.WaveJadePlugin(\.|$)') { return 'COE' }
    if ($rest -match '^common\.charger(\.|$)')              { return 'COE' }
    if ($rest -match '^client\.WaveEntityRendererRegistration(\.|$)') { return 'COE' }
    if ($rest -match '^content\.charger\.')     { return 'COE' }
    if ($rest -match '^content\.wave\.api\.')   { return 'COE' }
    if ($rest -match '^content\.energyfield\.') { return 'COE' }
    if ($rest -match '^common\.registry\.cews\.')           { return 'CEWS' }
    if ($rest -match '^content\.wave\.(block|frame|gauge|regulation)\.') { return 'CEWS' }
    if ($rest -match '^content\.machine\.')                 { return 'CEWS' }
    if ($rest -match '^client\.renderer\.(cews|wave)\.')    { return 'CEWS' }
    if ($rest -match '^client\.cews\.')                     { return 'CEWS' }
    if ($rest -match '^compat\.jei\.cews\.')                { return 'CEWS' }
    # W6-b2: the third layer is one file again (its empty @Mod shell); everything
    # else that used to be judged TRANS is COE now (see Get-FileLayer).
    if ($rest -match '^transmutation\.TransmutationMod(\.|$)') { return 'TRANS' }
    if ($rest -match '^common\.registry\.transmutation\.')  { return 'COE' }
    if ($rest -match '^content\.(transmuting|transmutation)\.')        { return 'COE' }
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
    if ($rest -match '^compat\.jei\.transmutation(\.|$)')            { return 'COE' }
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
$filesPerModule = [ordered]@{}
foreach ($root in $roots) {
    $filesPerModule[$root.Module] = 0
    foreach ($f in (Get-ChildItem -Recurse -Path $root.Path -Filter *.java | Sort-Object FullName)) {
        [void]$entries.Add(@{
            Module = $root.Module
            Path   = $f.FullName
            Rel    = $root.Prefix + $f.FullName.Substring($root.Path.Length + 1)
        })
        $filesPerModule[$root.Module] = $filesPerModule[$root.Module] + 1
    }
}

# ---------------------------------------------------------------------------
# M1 (W8-b): each module contributes at least one file and only contains layers it is
# allowed to contain.  See the two tables above and the header note.
# ---------------------------------------------------------------------------
$moduleProblems = New-Object System.Collections.ArrayList
$moduleLayers   = @{}
foreach ($m in $moduleAllowedLayers.Keys) {
    $moduleLayers[$m] = @{}
}
foreach ($e in $entries) {
    $l = Get-FileLayer $e.Rel
    if (-not $moduleLayers[$e.Module].ContainsKey($l)) { $moduleLayers[$e.Module][$l] = 0 }
    $moduleLayers[$e.Module][$l]++
}

$emptyModules = @()
foreach ($m in $moduleAllowedLayers.Keys) {
    if ([int]$filesPerModule[$m] -lt 1) { $emptyModules = $emptyModules + $m }
}
if ($emptyModules.Count -gt 0) {
    [void]$moduleProblems.Add('module(s) contributed no .java file at all: [' + ($emptyModules -join ',') + '] -- that module is not audited, and its layer counts read 0 while the tool still exits 0')
}

foreach ($m in $moduleAllowedLayers.Keys) {
    $unexpected = @()
    foreach ($l in @($moduleLayers[$m].Keys)) {
        if ($moduleAllowedLayers[$m] -notcontains $l) {
            $unexpected = $unexpected + ($l + '=' + $moduleLayers[$m][$l])
        }
    }
    if ($unexpected.Count -gt 0) {
        $sample = @($entries | Where-Object { $_.Module -eq $m -and ($moduleAllowedLayers[$m] -notcontains (Get-FileLayer $_.Rel)) } |
            Select-Object -First 5 | ForEach-Object { $_.Rel })
        [void]$moduleProblems.Add('module ' + $m + ' contains layer(s) it may not contain: [' + ($unexpected -join ',') + '] (allowed: [' + ($moduleAllowedLayers[$m] -join ',') + ']) e.g. [' + ($sample -join '; ') + '] -- the file ships in the wrong Gradle module; check-layering would still judge it by its path, so nothing else notices')
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

# ---------------------------------------------------------------------------
# W12-b (2026-09-30): wildcard imports are now resolved EXPLICITLY.
#
# A correction to an earlier reading of this very code, kept because the mistake is
# instructive: pass 1's name class is `[A-Za-z0-9_.]`, which cannot match `*`, so
# `import ...common.*;` never matches pass 1 -- and that was briefly read as "this
# gate is blind to wildcard imports".  It is NOT blind.  Pass 2 scans that same line
# as a fully-qualified occurrence, and because the FQN pattern is greedy it swallows
# the dot before `*`, yielding a bare-package FQN like `...content.wave.block.` whose
# trailing dot happens to satisfy the `(\.|$)` alternatives in Get-TargetLayer.
# MEASURED, not reasoned: with a deliberately injected COE -> CEWS wildcard import,
# the pre-change gate exits 1.  So the old coverage was real -- but ACCIDENTAL, and
# reported under the tag "fully-qualified, not an import" rather than as a wildcard.
#
# What this change therefore adds is narrower than a bug fix, but real:
#   1. wildcard edges come from an explicit branch, not from a greedy-match accident
#      that a future edit to the FQN pattern could silently remove;
#   2. they are labelled as wildcard edges, so a reader can tell them apart;
#   3. a wildcard expands to ONE target per layer present in the package, so a
#      package spanning two layers is no longer judged by a single bare-package FQN;
#   4. a wildcard naming neither a scanned package nor a scanned class is RED.
# Verdicts on the current tree are unchanged (EXIT=0 before and after; the 8 wildcard
# imports in this tree are all legal: 7 x `...createoreexpansion.common.*`, 1 x
# `...foundation.item.skill.*`).
#
# Sibling tool note: tools/layer-usage.ps1 has always resolved wildcards explicitly
# ("depends on EVERY class of that package"), so this gate was the divergent one; the
# expansion below follows that tool's intent while judging layers through the same
# Get-TargetLayer the named-import pass uses.
#
#   $classLayers   : fqn -> layer, for every scanned class.
#   $packageLayers : package -> set of layers its classes live in.  A wildcard
#                    expands to ONE target per layer present in the package.
# ---------------------------------------------------------------------------
$classLayers = @{}
$packageLayers = @{}
foreach ($e in $entries) {
    $r = $e.Rel
    if ($r -like 'core\*') { $r = $r.Substring(5) }
    $fqn = $prefix + (($r -replace '\\', '.') -replace '\.java$', '')
    # same trim-until-known-core walk Get-TargetLayer uses for nested classes
    $layer = Get-TargetLayer $fqn
    $classLayers[$fqn] = $layer
    $dot = $fqn.LastIndexOf('.')
    if ($dot -lt $prefix.Length) { continue }
    $pkg = $fqn.Substring(0, $dot)
    if (-not $packageLayers.ContainsKey($pkg)) {
        $packageLayers[$pkg] = New-Object System.Collections.Generic.HashSet[string]
    }
    [void]$packageLayers[$pkg].Add($layer)
}

$wildImports  = 0   # wildcard imports seen by the STRICT regex below (anti-vacuity guard)
$wildRawCount = 0   # same lines counted by a deliberately LOOSER, independent pattern
$wildResolved = 0   # of those, how many resolved to at least one layer
$guardProblems = New-Object System.Collections.ArrayList

$violations = New-Object System.Collections.ArrayList
$moduleEdges = New-Object System.Collections.ArrayList
$stats = @{ 'COE' = 0; 'CEWS' = 0; 'TRANS' = 0; 'SHARED' = 0; 'CORE' = 0 }
$edgeStats = @{}

foreach ($e in $entries) {
    $rel = $e.Rel
    # W8-b (M2): the PATH decides the layer, but a SHARED path carries no layer at all --
    # so for direction purposes such a file is judged by the MODULE it ships in.  Without
    # this, a `common/**` file inside :coe could import :transmutation and both the
    # "SHARED is never a source" rule and the COE -> TRANS rule would stay silent.
    $fromPath = Get-FileLayer $rel
    $from = $fromPath
    if ($fromPath -eq 'SHARED') { $from = $moduleOwnLayer[$e.Module] }
    $stats[$fromPath]++
    if ($from -eq 'SHARED' -or $from -eq '') { continue }        # the integration layer is unconstrained
    $moduleJudge = ($fromPath -ne $from)

    $lines = [System.IO.File]::ReadAllLines($e.Path, [System.Text.Encoding]::UTF8)
    $code = Get-CodeOnly $lines

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $lineNo = $i + 1
        $isImport = $false
        $targets = New-Object System.Collections.ArrayList

        # A comment line is never an edge.  Pass 2 has always skipped these; the wildcard
        # branch below must too, or a commented-out wildcard import would be reported as a
        # live dependency -- a false positive of exactly the kind this repo has learned to
        # ignore ("a permanently red gate gets ignored").
        $isCommentLine = ($line.TrimStart() -match '^(\*|//|/\*)')

        # ---- pass 1: import declarations
        $m = [regex]::Match($line, '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)\s*;')
        if ($m.Success) {
            $isImport = $true
            [void]$targets.Add($m.Groups[2].Value)
        }
        elseif (-not $isCommentLine) {
            # W12-b: `import <pkg>.*;` -- pass 1's name class contains no `*`, so the branch
            # above cannot match it.  A wildcard is a real
            # dependency on every layer present in the named package (or, for a static
            # member wildcard, on the named class), so it is expanded into one target per
            # layer and then judged by the ordinary Test-Forbidden path.
            $mw = [regex]::Match($line, '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)\.\*\s*;')
            if ($mw.Success) {
                $isImport = $true
                $wildImports++
                $wildName = $mw.Groups[2].Value
                $wildLayers = @()
                if ($packageLayers.ContainsKey($wildName)) {
                    $wildLayers = @($packageLayers[$wildName])
                }
                elseif ($classLayers.ContainsKey($wildName)) {
                    # `import static a.b.SomeClass.*;` -- depends on that class's layer
                    $wildLayers = @($classLayers[$wildName])
                }
                if ($wildLayers.Count -gt 0) {
                    $wildResolved++
                    foreach ($wl in $wildLayers) {
                        [void]$targets.Add(@{ Fqn = ($wildName + '.*'); Layer = $wl; Wild = $true })
                    }
                }
                else {
                    # Names neither a scanned package nor a scanned class: this gate cannot
                    # tell what it depends on, so it must not report "clean".
                    [void]$guardProblems.Add(('{0}:{1} wildcard import {2}.* matches NO scanned package or class -- its layer cannot be resolved, so it cannot be proven legal' -f $rel, $lineNo, $wildName))
                }
            }
        }

        # W12-b: independent, deliberately LOOSER recount of this same line.  If the strict
        # regex above ever stops matching (someone edits it, or the namespace moves) the two
        # counts diverge and the guard goes RED, instead of the wildcard branch quietly going
        # dead.  The pattern is looser ON PURPOSE: it must not share the strict regex's
        # failure mode.  Comment lines are excluded so the two counts stay comparable.
        if (-not $isCommentLine -and $line -match '^\s*import\s+(static\s+)?com\.hjmmd_8\.createoreexpansion\..*\.\*\s*;') {
            $wildRawCount++
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

        foreach ($t in $targets) {
            # $t is either a plain FQN string (named import / fully-qualified reference) or,
            # for a wildcard import, a hashtable carrying one resolved layer (W12-b).
            $isWild = $false
            if ($t -is [hashtable]) { $fqn = $t.Fqn; $to = $t.Layer; $isWild = $true }
            else { $fqn = $t; $to = Get-TargetLayer $fqn }
            if (-not (Test-Forbidden $from $to)) { continue }
            $key = "$rel`:$lineNo"
            $wlReason = $null
            if ($whitelist.Contains($key)) { $wlReason = $whitelist[$key] }
            elseif ($whitelist.Contains($rel)) { $wlReason = $whitelist[$rel] }
            if ($wlReason) { continue }

            $tag = ''
            if ($isWild) {
                # A wildcard edge is real but coarser than a named one, so it is labelled:
                # the reader must be able to tell "this file wildcard-imports a package that
                # spans a forbidden layer" from "this file names a forbidden class".
                $tag = (' [wildcard import: names a package/class that resolves to layer {0}]' -f $to)
            }
            elseif (-not $isImport) { $tag = ' [fully-qualified, not an import]' }
            else {
                $simple = ($fqn -split '\.')[-1]
                if ($code -notmatch ('(?<![\w.])' + [regex]::Escape($simple) + '(?![\w])')) {
                    $tag = ' [dead import: simple name never used in code]'
                }
            }
            $edge = "$from -> $to"
            if (-not $edgeStats.ContainsKey($edge)) { $edgeStats[$edge] = 0 }
            $edgeStats[$edge]++
            if ($moduleJudge) {
                # W8-b (M2): reported separately, with the MODULE named, because the file's
                # PATH says SHARED -- that is exactly why it escaped until now.
                [void]$moduleEdges.Add(("{0} [{1}, path-judged SHARED]{2} -> {3}{4}   ({5})" -f $rel, $e.Module, (":$lineNo"), $to, $tag, $fqn))
            }
            [void]$violations.Add(("{0}:{1} -> {2}{3}   ({4})" -f $rel, $lineNo, $to, $tag, $fqn))
        }
    }
}

Write-Host 'layering check: src/main/java + core/src/main/java + coe/src/main/java + transmutation/src/main/java + cews/src/main/java'
Write-Host ('  files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}  CORE(library)={4}' -f $stats['COE'], $stats['CEWS'], $stats['TRANS'], $stats['SHARED'], $stats['CORE'])
Write-Host '  forbidden edge directions checked : COE->CEWS, COE->TRANS, TRANS->CEWS, CEWS->TRANS, CORE->COE/CEWS/TRANS'
Write-Host '  W8-b: a SHARED-path file is additionally judged by the MODULE it ships in (M2)'
foreach ($m in $moduleAllowedLayers.Keys) {
    $lset = @($moduleLayers[$m].Keys | Sort-Object)
    Write-Host ('  module {0,-14} files={1,-4} layers={{ {2} }}  allowed={{ {3} }}' -f `
        $m, $filesPerModule[$m], ($lset -join ', '), ($moduleAllowedLayers[$m] -join ', '))
}
if ($NoFqn) { Write-Host '  (fully-qualified-reference pass disabled by -NoFqn)' }

# ---------------------------------------------------------------------------
# M1 / M2 (W8-b) verdicts.  M1 problems are their own list because they are a different
# kind of statement ("this file is in the wrong module") from a direction violation.
# ---------------------------------------------------------------------------
if ($moduleProblems.Count -gt 0) {
    Write-Host ''
    Write-Host ('MODULE/LAYER MISMATCHES ({0}):' -f $moduleProblems.Count)
    foreach ($p in $moduleProblems) { Write-Host ('  ' + $p) }
}

if ($moduleEdges.Count -gt 0) {
    Write-Host ''
    Write-Host ('MODULE-LEVEL DIRECTION VIOLATIONS ({0}) -- file sits on a SHARED path, so the old rule saw nothing:' -f $moduleEdges.Count)
    foreach ($v in ($moduleEdges | Sort-Object)) { Write-Host ('  ' + $v) }
}

# ---------------------------------------------------------------------------
# W12-b anti-vacuity guard (same discipline as tools/check-module-selfsufficiency.ps1:
# a check that matches nothing must go RED, never pass by default).  If the tree HAS
# wildcard imports but the resolver above resolved NONE of them, the new branch is dead
# and this gate is blind again in exactly the way W12-b was written to remove.
# ---------------------------------------------------------------------------
if ($wildImports -gt 0 -and $wildResolved -eq 0) {
    [void]$guardProblems.Add(('wildcard-import resolver is DEAD: the tree has {0} wildcard import(s) but 0 resolved -- each would have needed a key in $packageLayers/$classLayers, so the guard is not doing its job' -f $wildImports))
}
if ($wildRawCount -ne $wildImports) {
    [void]$guardProblems.Add(('wildcard-import scan is INCOMPLETE: the loose pattern counted {0} wildcard import line(s) but the strict scanner matched {1} -- the two must agree, or the scan has a blind spot it cannot see' -f $wildRawCount, $wildImports))
}
if ($guardProblems.Count -gt 0) {
    Write-Host ''
    Write-Host ('GUARD PROBLEMS ({0}) -- the checker could not prove something it is supposed to prove:' -f $guardProblems.Count)
    foreach ($p in $guardProblems) { Write-Host ('  ' + $p) }
}

if ($violations.Count -eq 0 -and $moduleProblems.Count -eq 0 -and $guardProblems.Count -eq 0) {
    Write-Host ('OK: no forbidden cross-layer dependency found (M1 module/layer sets OK, M2 module-level edges 0; wildcard imports seen={0} resolved={1}).' -f $wildImports, $wildResolved)
    exit 0
}

if ($violations.Count -gt 0) {
    Write-Host ''
    Write-Host ('VIOLATIONS ({0}):' -f $violations.Count)
    foreach ($v in ($violations | Sort-Object)) { Write-Host ('  ' + $v) }
    Write-Host ''
    Write-Host 'by direction:'
    foreach ($k in ($edgeStats.Keys | Sort-Object)) { Write-Host ('  {0} : {1}' -f $k, $edgeStats[$k]) }
}
if ($whitelist.Count -eq 0) { Write-Host 'whitelist: (empty)' }
else { Write-Host ('whitelist entries: ' + $whitelist.Count) }
exit 1
