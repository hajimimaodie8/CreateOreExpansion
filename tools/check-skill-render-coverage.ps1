# check-skill-render-coverage.ps1 -- strategy-renderer coverage assertion for createoreexpansion.
#
# Closes the "preview silently disappears" hole.  A skill that was migrated into Skiller
# (registered in skiller:skill AND implementing StrategySkill) but whose strategy renderer id
# is never registered via StrategyRenderers.register(...) renders NOTHING -- and it is a pure
# run-time symptom: compileJava is green, runData is green, and the upstream warning
# (StrategyRenderers.MISSING_RENDERER_WARNED) does NOT fire, because that warning only covers
# a strategy that WAS scheduled but has no renderer.  A renderer id that nobody registered
# and nobody scheduled produces no log line at all.
#
# Assertions (all source-level, read-only):
#   1. register-count : the number of StrategyRenderers.register(...) calls in
#                       CoeSkillClient equals the number of DISTINCT renderer ids declared by
#                       RENDERER_ID constants under integration/skiller/strategy/**.
#   2. declared==used : every declared renderer id is registered AND every registered id is
#                       declared (no orphan constant, no dead registration).  Alias constants
#                       (RENDERER_ID = OtherStrategy.RENDERER_ID) are resolved.
#   3. skill-coverage : for every skill class instantiated in SkillerIntegration that
#                       implements StrategySkill, the renderer id of the strategy it
#                       references (XxxStrategy.KEY / .ID) must be in the registered set.
#                       This is the "migrated a skill, forgot its renderer" trap.
#   4. slot-gate      : the slot gate must exist in BOTH outline renderers -- each of
#                       CoeBlockOutlineRenderer.java and CoeEntityOutlineRenderer.java must
#                       read ClientSkillCache's bindings() (the only place a key slot exists,
#                       ISkillInstance has no slot()).  Without it the renderer falls back to
#                       "any skill key is pressed" and draws every same-family strategy at
#                       once, so the preview stops matching the release path (which is
#                       strictly per slot).
#
# Exit code: 0 = every assertion passed, 1 = at least one failed.
#
# NOTE: this file is deliberately pure ASCII.  Windows PowerShell 5.1 reads a BOM-less .ps1
# as ANSI, which mangles non-ASCII text and can even swallow quotes mid-script.

$ErrorActionPreference = 'Stop'

$repoRoot    = Split-Path -Parent $PSScriptRoot
$coePkgRoot  = Join-Path $repoRoot 'coe\src\main\java\com\hjmmd_8\createoreexpansion'
$skillerRoot = Join-Path $coePkgRoot 'integration\skiller'
$clientDir   = Join-Path $skillerRoot 'client'
$strategyDir = Join-Path $skillerRoot 'strategy'
$skillDir    = Join-Path $skillerRoot 'skill'

$integrationFile = Join-Path $skillerRoot 'SkillerIntegration.java'
$clientFile      = Join-Path $clientDir   'CoeSkillClient.java'
$blockRenderer   = Join-Path $clientDir   'CoeBlockOutlineRenderer.java'
$entityRenderer  = Join-Path $clientDir   'CoeEntityOutlineRenderer.java'

foreach ($p in @($strategyDir, $skillDir, $integrationFile, $clientFile, $blockRenderer, $entityRenderer)) {
    if (-not (Test-Path $p)) { throw "required path not found: $p" }
}

$failures = New-Object System.Collections.Generic.List[string]
$checks   = New-Object System.Collections.Generic.List[string]

function Read-Text([string]$path) {
    return (Get-Content -Raw -Encoding UTF8 $path)
}

# Structural matching must never be fooled by comments: a commented-out
# `StrategyRenderers.register(...)` line, or a javadoc example of one, is NOT a
# registration.  So every regex below runs on comment-stripped source.
#
# W8-b (2026-09-30): the strip used to be two sequential regex passes --
#   $text = [regex]::Replace($text, '/\*.*?\*/', '')    # block comments FIRST
#   $text = [regex]::Replace($text, '//[^\r\n]*', '')   # line comments SECOND
# -- and that order fails in the quiet direction.  If a LINE comment contains "/*"
# (an ordinary thing to write, e.g. "// see strategy/**"), the block pass pairs that
# "/*" with the NEXT "*/" anywhere later in the file and deletes everything between,
# real code included.  The mirror order is no better: a "//" inside a block comment
# makes the line pass delete the "*/" that closes it, leaving an unterminated "/*"
# that can pair with a LATER "*/".  This is the SAME defect W6-d fixed in
# tools\check-module-selfsufficiency.ps1 (Get-SourceCode), found by the same survey.
# The replacement is ONE left-to-right pass with an alternation, i.e. the exact
# semantics "strip whichever comment starts first":
#     (?s:/\*.*?\*/)   a block comment, closed at its FIRST "*/"
#     (?m://.*$)       a line comment, to the end of the line
# .NET scans left to right and takes the LEFTMOST match, so neither order problem can
# occur.  This is a STRICTENING: the visible-text set is a superset of what either
# sequential order produced for real code.
# Remaining, documented imprecision (unchanged): a "//" inside a STRING literal still
# cuts the rest of that line.  Closing that needs a real tokenizer; a buggy tokenizer
# is a much worse failure mode than this known, one-line-bounded one.
function Get-Code([string]$path) {
    $text = Read-Text $path
    $text = [regex]::Replace($text, '(?s:/\*.*?\*/)|(?m://.*$)', '')
    return $text
}

# Resolve a RENDERER_ID right-hand side to a canonical id.  The canonical id is the string
# literal of the ResourceLocation path (e.g. "block_outline"), because that is what decides
# whether two constants address the same renderer.  An alias (`OtherStrategy.RENDERER_ID`)
# is followed recursively.
function Resolve-RendererId([string]$className, [hashtable]$raw, [int]$depth) {
    if ($depth -gt 8) { return $null }
    if (-not $raw.ContainsKey($className)) { return $null }
    $rhs = $raw[$className]
    $alias = [regex]::Match($rhs, '^([A-Za-z0-9_]+)\s*\.\s*RENDERER_ID$')
    if ($alias.Success) { return (Resolve-RendererId $alias.Groups[1].Value $raw ($depth + 1)) }
    $literals = [regex]::Matches($rhs, '"([^"]*)"')
    if ($literals.Count -eq 0) { return $null }
    return $literals[$literals.Count - 1].Groups[1].Value
}

# ---------------------------------------------------------------------------
# 1. every strategy that declares a RENDERER_ID constant
# ---------------------------------------------------------------------------
$rawIds = @{}          # strategy class name -> raw right-hand side
foreach ($file in (Get-ChildItem -Path $strategyDir -Filter *.java -File | Sort-Object Name)) {
    $text = Get-Code $file.FullName
    $m = [regex]::Match($text, 'public\s+static\s+final\s+ResourceLocation\s+RENDERER_ID\s*=\s*([^;]+);')
    if (-not $m.Success) { continue }
    $rawIds[$file.BaseName] = $m.Groups[1].Value.Trim()
}
if ($rawIds.Count -eq 0) {
    $failures.Add("no RENDERER_ID constant found under integration/skiller/strategy/**")
}

$declaredIds = @{}     # canonical id -> declaring strategy class
foreach ($cls in ($rawIds.Keys | Sort-Object)) {
    $canon = Resolve-RendererId $cls $rawIds 0
    if ([string]::IsNullOrEmpty($canon)) {
        $failures.Add("RENDERER_ID of $cls does not resolve to a ResourceLocation path: '$($rawIds[$cls])'")
        continue
    }
    if ($declaredIds.ContainsKey($canon)) {
        # Two constants may share one id on purpose (CoeFellingStrategy aliases CoeAreaAoeStrategy).
        $declaredIds[$canon] = "$($declaredIds[$canon]),$cls"
    } else {
        $declaredIds[$canon] = $cls
    }
}

# ---------------------------------------------------------------------------
# 2. every registration in CoeSkillClient
# ---------------------------------------------------------------------------
$clientText    = Get-Code $clientFile
$registerCalls = [regex]::Matches($clientText, 'StrategyRenderers\s*\.\s*register\s*\(')
$registeredIds = @{}   # canonical id -> registering strategy class
$registeredClasses = New-Object System.Collections.Generic.List[string]
foreach ($m in [regex]::Matches($clientText, 'StrategyRenderers\s*\.\s*register\s*\(\s*([A-Za-z0-9_]+)\s*\.\s*RENDERER_ID')) {
    $cls = $m.Groups[1].Value
    $registeredClasses.Add($cls)
    $canon = Resolve-RendererId $cls $rawIds 0
    if ([string]::IsNullOrEmpty($canon)) {
        $failures.Add("StrategyRenderers.register($cls.RENDERER_ID, ...) uses an id that does not resolve")
        continue
    }
    $registeredIds[$canon] = $cls
}

# Assertion 1: register call count == number of distinct declared renderer ids.
$expectedCalls = $declaredIds.Count
$actualCalls   = $registerCalls.Count
if ($actualCalls -eq $expectedCalls) {
    $checks.Add("1. register-count      PASS  ($actualCalls StrategyRenderers.register call(s) == $expectedCalls distinct declared renderer id(s))")
} else {
    $checks.Add("1. register-count      FAIL  ($actualCalls StrategyRenderers.register call(s) != $expectedCalls distinct declared renderer id(s))")
    $failures.Add("register call count $actualCalls != distinct declared renderer ids $expectedCalls")
}

# Assertion 2: declared set == registered set (both directions).
$missing = @($declaredIds.Keys | Where-Object { -not $registeredIds.ContainsKey($_) } | Sort-Object)
$extra   = @($registeredIds.Keys | Where-Object { -not $declaredIds.ContainsKey($_) } | Sort-Object)
if ($missing.Count -eq 0 -and $extra.Count -eq 0) {
    $checks.Add("2. declared==used      PASS  (declared: $((($declaredIds.Keys | Sort-Object) -join ', ')))")
} else {
    $checks.Add("2. declared==used      FAIL  (unregistered: $($missing -join ', '); undeclared: $($extra -join ', '))")
    foreach ($id in $missing) { $failures.Add("renderer id '$id' is declared by strategy $($declaredIds[$id]) but never registered") }
    foreach ($id in $extra)   { $failures.Add("renderer id '$id' is registered by $($registeredIds[$id]) but no strategy declares it") }
}

# ---------------------------------------------------------------------------
# 3. every migrated StrategySkill must reach a registered renderer
# ---------------------------------------------------------------------------
$integrationText = Get-Code $integrationFile
$skillClasses = New-Object System.Collections.Generic.List[string]
foreach ($m in [regex]::Matches($integrationText, 'new\s+([A-Za-z0-9_]+)\s*[\(\.]')) {
    $name = $m.Groups[1].Value
    if ($name -like '*Skill') { $skillClasses.Add($name) }
}
$skillClasses = @($skillClasses | Sort-Object -Unique)
if ($skillClasses.Count -eq 0) {
    $failures.Add("no 'new XxxSkill(...)' found in SkillerIntegration.java (parser drift?)")
}

$coverageChecked = 0
foreach ($cls in $skillClasses) {
    $path = Join-Path $skillDir "$cls.java"
    if (-not (Test-Path $path)) {
        $failures.Add("skill class $cls is instantiated by SkillerIntegration but $cls.java is missing")
        continue
    }
    $text = Get-Code $path
    # Plain ItemSkill implementations need no renderer; only StrategySkill ones are scheduled.
    if ($text -notmatch 'implements[^{]*\bStrategySkill\b') { continue }
    $coverageChecked++
    $refs = @()
    foreach ($m in [regex]::Matches($text, '([A-Za-z0-9_]+Strategy)\s*\.\s*(?:KEY|ID)\b')) {
        $refs += $m.Groups[1].Value
    }
    $refs = @($refs | Sort-Object -Unique)
    if ($refs.Count -eq 0) {
        $failures.Add("$cls implements StrategySkill but references no strategy key (no XxxStrategy.KEY)")
        continue
    }
    foreach ($ref in $refs) {
        if (-not $rawIds.ContainsKey($ref)) {
            $failures.Add("$cls references strategy $ref, which declares no RENDERER_ID")
            continue
        }
        $id = Resolve-RendererId $ref $rawIds 0
        if (-not $registeredIds.ContainsKey($id)) {
            $failures.Add("$cls -> $ref -> renderer id '$id' is NOT registered in CoeSkillClient (preview would silently not render)")
        }
    }
}
if ($coverageChecked -gt 0) {
    $checks.Add("3. skill-coverage      PASS  ($coverageChecked StrategySkill class(es) checked, all reach a registered renderer)")
} else {
    $checks.Add("3. skill-coverage      FAIL  (no StrategySkill class was checked at all)")
    $failures.Add("no StrategySkill class found among the skills instantiated by SkillerIntegration")
}

# ---------------------------------------------------------------------------
# 4. slot gate present in BOTH renderers
# ---------------------------------------------------------------------------
$slotGateFailuresBefore = $failures.Count
foreach ($renderer in @($blockRenderer, $entityRenderer)) {
    $leaf = Split-Path -Leaf $renderer
    $text = Get-Code $renderer
    if ($text -notmatch 'bindings\s*\(\s*\)') {
        $failures.Add("slot gate missing: $leaf never reads bindings() (falls back to 'any skill key')")
    }
    if ($text -notmatch 'ClientSkillCache') {
        $failures.Add("slot gate missing: $leaf does not use ClientSkillCache (the only slot source)")
    }
}
if ($failures.Count -eq $slotGateFailuresBefore) {
    $checks.Add("4. slot-gate           PASS  (bindings() slot lookup present in CoeBlockOutlineRenderer + CoeEntityOutlineRenderer)")
} else {
    $checks.Add("4. slot-gate           FAIL  (see failures below)")
}

# ---------------------------------------------------------------------------
# report
# ---------------------------------------------------------------------------
Write-Output "check-skill-render-coverage: $($checks.Count) assertion(s)"
foreach ($c in $checks) { Write-Output "  $c" }
Write-Output ""
if ($failures.Count -eq 0) {
    Write-Output "SKILL-RENDER-COVERAGE: OK"
    exit 0
}
Write-Output "SKILL-RENDER-COVERAGE: $($failures.Count) failure(s)"
foreach ($f in $failures) { Write-Output "  - $f" }
exit 1
