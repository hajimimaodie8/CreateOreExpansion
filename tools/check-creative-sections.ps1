# Creative-page section banner check (COE base_tab: "one page + three section banners").
#
# WHY THIS EXISTS
#   The banner feature is a CHAIN of small, individually-valid pieces:
#     sprite files  ->  sections table  ->  sectioned tab subclass  ->  core injection point
#     ->  client renderer  ->  three lang keys
#   Break any single link and nothing throws: the page still opens, still lists every item.
#   The only symptom is "a banner is missing", which no compiler and no other gate notices.
#   The links that are easiest to lose by accident:
#     * someone edits core/LayerCreativeTab and drops the withTabFactory branch
#     * someone declares .sectioned(...) on the CEWS tab too (its order is pinned elsewhere!)
#     * someone renames a sprite file or moves it out of textures/gui/sprites/ (atlas only
#       scans that directory, and a miss degrades SILENTLY to the purple/black missing sprite)
#     * someone adds a section to BASE_SECTIONS without a matching language key
#   Each assertion below carries a match-count floor so a broken scan goes RED, not green.
#
# Pure ASCII. Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-creative-sections.ps1
param([string]$Repo = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$fails = 0

function Fail([string]$id, [string]$why) {
    Write-Output "FAIL[$id]: $why"
    $script:fails++
}

function Read-Text([string]$rel) {
    $path = Join-Path $Repo $rel
    if (-not (Test-Path $path)) { return $null }
    # Comments are stripped for every source read: see Strip-Comments below.
    return (Strip-Comments (Get-Content $path -Raw -Encoding UTF8))
}

# Strip line and block comments before pattern checks.
# WHY: prose in this codebase discusses the very things these assertions look for
#      (e.g. a javadoc line saying "do NOT use Math.round here"), and a naive match
#      would flag the comment and pass a real regression. Learned the hard way.
function Strip-Comments([string]$text) {
    if ($null -eq $text) { return $null }
    $noBlock = [regex]::Replace($text, '/\*.*?\*/', '', 'Singleline')
    return [regex]::Replace($noBlock, '//[^\r\n]*', '')
}

$SECTIONS = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/CoeCreativeSections.java'
$TAB      = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/CoeSectionedTab.java'
$TABS     = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/CoeCreativeTabs.java'
$RENDER   = 'coe/src/main/java/com/hjmmd_8/createoreexpansion/client/creative/CoeCreativeSectionBanners.java'
$CORE     = 'core/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/LayerCreativeTab.java'
$SPRITEDIR = 'coe/src/main/resources/assets/createoreexpansion/textures/gui/sprites'

# 2026-09-30 (author's ruling "keep a single tab"): the CEWS creative tab and its pinning class
# were DELETED, and the CEWS layer's items now default into this page's machine section.
# These two paths must stay gone, and the CEWS registrate must point at the single tab.
$CEWS_TAB_FILE = 'cews/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/cews/CewsCreativeTabs.java'
$CEWS_PIN_FILE = 'cews/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/cews/EnergyWaveStudyTab.java'
$CEWS_REG      = 'cews/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/cews/CewsRegistrate.java'
$CEWS_MOD      = 'cews/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/cews/CewsMod.java'

$sections = Read-Text $SECTIONS
if ($null -eq $sections) { Fail 'sections-file' "missing $SECTIONS"; }
else {
    # --- 1) the three sections, in the author-mandated order ore -> machine -> gear --------
    $order = [regex]::Matches($sections, 'new Section\("([a-z_]+)"') | ForEach-Object { $_.Groups[1].Value }
    if ($order.Count -ne 3) {
        Fail 'section-count' "expected 3 Section declarations, found $($order.Count)"
    } elseif (($order -join ',') -ne 'ore,machine,gear') {
        Fail 'section-order' "section order must be ore,machine,gear (author requirement), found $($order -join ',')"
    } else {
        Write-Output "ok  [section-order] ore -> machine -> gear"
    }

    # --- 2) layout() keeps the three invariants that make a banner row exist --------------
    if ($sections -notmatch 'for \(int i = 0; i < ITEMS_PER_ROW; i\+\+\)') {
        Fail 'layout-leading-row' "layout() must pad ONE full empty row up front (the first banner lives on row 0)"
    } else { Write-Output "ok  [layout-leading-row]" }

    if ($sections -notmatch 'ITEMS_PER_ROW - used\) % ITEMS_PER_ROW \+ ITEMS_PER_ROW') {
        Fail 'layout-pad' "layout() pad must be (9 - n%9)%9 + 9 so every section ends with a blank row"
    } else { Write-Output "ok  [layout-pad]" }

    if ($sections -notmatch 'SECTION_ROWS\.put\(tabKey \+ "\|" \+ bucket\.key\(\), out\.size\(\) / ITEMS_PER_ROW\)') {
        Fail 'layout-row-record' "the recorded row must be out.size()/9 sampled BEFORE the bucket items are added"
    } else { Write-Output "ok  [layout-row-record]" }

    # --- 3) every declared section sprite exists under the ONLY atlas-scanned directory ---
    $sprites = [regex]::Matches($sections, 'sprite\("([a-z_]+)"\)') | ForEach-Object { $_.Groups[1].Value }
    if ($sprites.Count -ne 3) {
        Fail 'sprite-count' "expected 3 sprite(...) declarations, found $($sprites.Count)"
    } else {
        $spriteDirAbs = Join-Path $Repo $SPRITEDIR
        foreach ($s in $sprites) {
            $file = Join-Path $spriteDirAbs "$s.png"
            if (-not (Test-Path $file)) {
                Fail 'sprite-file' "sprite '$s' declared in code but missing: $SPRITEDIR/$s.png (blitSprite would degrade SILENTLY)"
            } elseif ((Get-Item $file).Length -le 0) {
                Fail 'sprite-file' "sprite file is empty: $SPRITEDIR/$s.png"
            }
        }
        if ($fails -eq 0) { Write-Output "ok  [sprite-file] $($sprites -join ', ') present in textures/gui/sprites/" }
    }

    # --- 4) the section-detection must stay rule based, not a 146-item enumeration --------
    $itemEquals = [regex]::Matches($sections, 'path\.Equals\("').Count + [regex]::Matches($sections, '"[a-z_]+"\.equals\(path\)').Count
    if ($itemEquals -gt 5) {
        Fail 'rule-based' "found $itemEquals literal item-name comparisons; detection must stay rule based (<=5 point-named exceptions)"
    } else {
        Write-Output "ok  [rule-based] $itemEquals point-named exception(s), the rest is tag/suffix driven"
    }
}

# --- 5) the sectioned tab subclass and its one override ----------------------------------
$tab = Read-Text $TAB
if ($null -eq $tab) { Fail 'tab-file' "missing $TAB" }
else {
    if ($tab -notmatch 'extends CreativeModeTab') { Fail 'tab-extends' "CoeSectionedTab must extend CreativeModeTab" }
    elseif ($tab -notmatch 'public Collection<ItemStack> getDisplayItems\(\)') {
        Fail 'tab-override' "CoeSectionedTab must override getDisplayItems()"
    } elseif ($tab -notmatch 'super\.getDisplayItems\(\)') {
        Fail 'tab-super' "the override must start from super.getDisplayItems() (the game's real list)"
    } elseif ($tab -notmatch 'bucketSum != items\.size\(\)') {
        Fail 'tab-guard' "the length guard is mandatory: a mismatch must fall back to the untouched list"
    } else {
        Write-Output "ok  [sectioned-tab] extends CreativeModeTab, overrides getDisplayItems(), keeps the length guard"
    }
}

# --- 6) wiring: exactly ONE layer declares .sectioned(...), and it is COE -----------------
$tabsText = Read-Text $TABS
if ($null -eq $tabsText) { Fail 'tabs-file' "missing $TABS" }
elseif ($tabsText -notmatch '\.sectioned\(CoeSectionedTab::new\)') {
    Fail 'tabs-sectioned' "CoeCreativeTabs.BASE_TAB must opt in with .sectioned(CoeSectionedTab::new)"
} else { Write-Output "ok  [tabs-sectioned] base_tab opts in" }

if ($null -ne (Read-Text $TABS)) {
    $tabsText = Read-Text $TABS
    if ($tabsText -notmatch 'List<LayerCreativeTab> TABS = List\.of\(BASE_TAB\)') {
        Fail 'single-tab' "CoeCreativeTabs must declare exactly one page (TABS = List.of(BASE_TAB)) -- the author keeps a single creative tab"
    } else {
        Write-Output "ok  [single-tab] one page declared"
    }
    if ($tabsText -notmatch 'PALETTES_CREATIVE_TAB\.getKey\(\)') {
        Fail 'anchor' "base_tab must anchor before Create's palettes page (it took over the removed CEWS tab's slot)"
    } else {
        Write-Output "ok  [anchor] base_tab anchors before Create's palettes page"
    }
}

# --- 6b) the CEWS tab must stay DELETED, and its layer must feed this page ---------------
foreach ($gone in @(
    @{ id = 'cews-tab-gone'; path = $CEWS_TAB_FILE; why = 'the CEWS creative tab was deleted by author ruling -- a second tab contradicts it' },
    @{ id = 'cews-tab-gone'; path = $CEWS_PIN_FILE; why = 'EnergyWaveStudyTab only existed to split items between two tabs; recreating it means two tabs again' }
)) {
    if (Test-Path (Join-Path $Repo $gone.path)) { Fail $gone.id "$($gone.path) exists: $($gone.why)" }
}
if (-not (Test-Path (Join-Path $Repo $CEWS_TAB_FILE)) -and -not (Test-Path (Join-Path $Repo $CEWS_PIN_FILE))) {
    Write-Output "ok  [cews-tab-gone] CewsCreativeTabs + EnergyWaveStudyTab are absent"
}
$cewsReg = Read-Text $CEWS_REG
if ($null -eq $cewsReg) { Fail 'cews-registrate' "missing $CEWS_REG" }
elseif ($cewsReg -notmatch 'defaultCreativeTab\(CoeCreativeTabs\.BASE_TAB\.key\(\)\)') {
    Fail 'cews-default-tab' "CewsRegistrate must default into CoeCreativeTabs.BASE_TAB (otherwise its machines land on no page at all)"
} else {
    Write-Output "ok  [cews-default-tab] CEWS items default into the single page"
}
$cewsMod = Read-Text $CEWS_MOD
if ($null -ne $cewsMod -and $cewsMod -match 'registerAll\(CewsCreativeTabs') {
    Fail 'cews-registration' "CewsMod must not register a CEWS tab any more"
}

# --- 6c) the CEWS wave machines must keep landing in the MACHINE section -----------------
$waveSuffixes = @('_field_controller', '_wave_transmuter', '_wave_regulator',
                  '_speed_regulator', '_disperser', '_differencer', '_query_gauge')
if ($null -ne $sections) {
    $missingRules = @()
    foreach ($suffix in $waveSuffixes) {
        if ($sections -notmatch [regex]::Escape('"' + $suffix + '"')) { $missingRules += $suffix }
    }
    if ($missingRules.Count -gt 0) {
        Fail 'wave-machine-rules' ("machine section has no rule for: " + ($missingRules -join ', ') + " -- those 12 CEWS machines would fall into the banner-less fallback bucket")
    } else {
        Write-Output "ok  [wave-machine-rules] all 7 wave-machine suffix families routed to the machine section"
    }
}

# --- 7) the core injection point must survive (core keeps ZERO layer references) --------
$core = Read-Text $CORE
if ($null -eq $core) { Fail 'core-file' "missing $CORE" }
else {
    if ($core -notmatch 'withTabFactory\(tab\.tabFactory\)') {
        Fail 'core-injection' "LayerCreativeTab.registerAll must still pass tabFactory to withTabFactory"
    } elseif ($core -notmatch 'public LayerCreativeTab sectioned\(') {
        Fail 'core-setter' "LayerCreativeTab#sectioned is the only supported way to swap the tab implementation"
    } elseif ($core -match 'createoreexpansion\.common\.registry\.coe\.') {
        Fail 'core-purity' "core must not import layer-specific classes (it may only hold a java.util.function.Function)"
    } else {
        Write-Output "ok  [core-injection] withTabFactory branch + sectioned() present, no layer import"
    }
}

# --- 8) client renderer: the three guards, the reflection targets, the adaptive row ------
$render = Read-Text $RENDER
if ($null -eq $render) { Fail 'render-file' "missing $RENDER" }
else {
    foreach ($need in @(
        @{ id = 'render-hook';      pattern = 'ContainerScreenEvent\.Render\.Foreground'; why = 'the banner MUST be drawn in the container Foreground hook (after items, BEFORE the tooltip)' },
        @{ id = 'render-screen-guard'; pattern = 'instanceof CreativeModeInventoryScreen'; why = 'guard 1: only the creative screen' },
        @{ id = 'render-tab-guard';    pattern = 'OUR_TAB_ID\.equals\(selectedTabId\(\)\)'; why = 'guard 2: only our own tab (never draw on someone else''s page)' },
        @{ id = 'render-rows-guard';   pattern = 'rows\.isEmpty\(\)'; why = 'guard 3: only when this page has section records' },
        @{ id = 'render-adaptive';     pattern = 'isRowEmpty\(menu, anchor\)'; why = 'adaptive "whole row blank" detection (never trust row arithmetic)' },
        @{ id = 'render-scroll';       pattern = 'scrollOffs \* \(float\) rowCount\) \+ 0\.5'; why = 'vanilla scroll formula: (int)(s*rowCount + 0.5), NOT Math.round' },
        @{ id = 'render-silent';       pattern = 'catch \(Throwable ignored\)'; why = 'decorative rendering must never crash the client' },
        @{ id = 'render-sprite';       pattern = 'blitSprite\(banner, x, y, BANNER_WIDTH, BANNER_HEIGHT\)'; why = 'blitSprite takes a SPRITE NAME, drawn 1:1 at 162x18' }
    )) {
        if ($render -notmatch $need.pattern) { Fail $need.id $need.why }
    }
    # 2026-09-30 user-reported defect: ScreenEvent.Render.Post fires AFTER renderWithTooltip,
    # so the banner covered the hovered item's tooltip. Foreground fires before it. Never go back.
    if ($render -match 'ScreenEvent\.Render\.Post') {
        Fail 'render-tooltip-clobber' "ScreenEvent.Render.Post is fired AFTER the tooltip is drawn -- the banner would cover hovered tooltips again (this was a real reported defect). Use ContainerScreenEvent.Render.Foreground."
    } else {
        Write-Output "ok  [render-tooltip-clobber] not registered on ScreenEvent.Render.Post"
    }
    # reflection must stay limited to the two private fields that have no public accessor
    $fieldNames = [regex]::Matches($render, 'findField\([^,]+,\s*"([A-Za-z]+)"\)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
    $allowed = @('scrollOffs', 'selectedTab')
    $unexpected = $fieldNames | Where-Object { $_ -notin $allowed }
    if ($unexpected) {
        Fail 'render-reflection' "unexpected reflected field(s): $($unexpected -join ', ') (only selectedTab + scrollOffs need reflection; leftPos/topPos use getGuiLeft/getGuiTop)"
    } elseif ($fieldNames.Count -ne 2) {
        Fail 'render-reflection' "expected exactly 2 reflected fields, found $($fieldNames.Count)"
    } else {
        Write-Output "ok  [render] 3 guards + adaptive row + vanilla scroll formula + silent fallback + 2 reflected fields only"
    }
    if ($render -match 'Math\.round') {
        Fail 'render-round' "Math.round would shift the row at scroll 0.75 (see the guide 6.6); use the vanilla truncating formula"
    }
}

# --- 9) language keys: one per section, in both generated language files ------------------
$langKeys = @()
if ($null -ne $sections) {
    $langKeys = [regex]::Matches($sections, 'LANG_PREFIX \+ "([a-z_]+)"') | ForEach-Object { $_.Groups[1].Value }
}
if ($langKeys.Count -ne 3) {
    Fail 'lang-key-count' "expected 3 language keys derived from LANG_PREFIX, found $($langKeys.Count)"
} else {
    foreach ($lang in @('zh_cn', 'en_us')) {
        $langFile = Join-Path $Repo "coe/src/generated/resources/assets/createoreexpansion/lang/$lang.json"
        if (-not (Test-Path $langFile)) { Fail 'lang-file' "missing $langFile"; continue }
        $json = Get-Content $langFile -Raw -Encoding UTF8
        foreach ($k in $langKeys) {
            if ($json -notmatch [regex]::Escape("createoreexpansion.creative_section.$k")) {
                Fail 'lang-key' "key createoreexpansion.creative_section.$k missing from $lang.json (banner label would show the raw key)"
            }
        }
    }
    if ($fails -eq 0) { Write-Output "ok  [lang] $($langKeys -join ', ') present in zh_cn + en_us" }
}

# --- 10) the three banner PNGs must stay untouched author assets (sizes are known) -------
$expected = @{ 'section_ore.png' = 1036; 'section_machine.png' = 1959; 'section_gear.png' = 3108 }
$checkedSizes = 0
foreach ($name in $expected.Keys) {
    $file = Join-Path $Repo "$SPRITEDIR/$name"
    if (Test-Path $file) {
        $len = (Get-Item $file).Length
        if ($len -ne $expected[$name]) {
            Fail 'sprite-bytes' "$name is $len B, expected $($expected[$name]) B -- the banner art is an author asset and must not be edited"
        }
        $checkedSizes++
    }
}
if ($checkedSizes -eq 3 -and $fails -eq 0) {
    Write-Output "ok  [sprite-bytes] all three banner textures byte-identical to the supplied art"
} elseif ($checkedSizes -ne 3) {
    Fail 'sprite-bytes' "expected to check 3 banner textures, checked $checkedSizes"
}

if ($fails -gt 0) {
    Write-Output ""
    Write-Output "FAIL: $fails creative-section assertion(s) violated."
    Write-Output "Symptom of a broken link here is SILENT: the page still opens and still lists every item,"
    Write-Output "only a banner goes missing. See docs/ (shared-experience) '08-creative page section banners'"
    Write-Output "sections 5 and 6 (the file name itself is Chinese: docs/<zh>/08-<zh>.md)."
    exit 1
}
Write-Output ""
Write-Output "OK: creative-page sections intact (order ore/machine/gear, layout invariants, 3 sprites in the"
Write-Output "    atlas-scanned dir with original bytes, one sectioned tab (COE only), core injection point,"
Write-Output "    client renderer guards + adaptive row, 3 language keys in both locales)."
exit 0
