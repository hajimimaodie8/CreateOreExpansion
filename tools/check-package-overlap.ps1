# check-package-overlap.ps1 -- JPMS package-collision audit for createoreexpansion.
#
# WHY THIS EXISTS
#   FML / ModLauncher turn every mod file -- including JarJar-nested ones -- into a JPMS
#   named module inside a single Configuration.  JPMS then enforces "one package belongs
#   to at most one module of a layer", and the failure mode is a hard startup crash:
#       java.lang.module.ResolutionException: Modules A and B export package X to module Y
#   Nothing static catches it on its own: compileJava, runData and runClient all stay
#   green, because in dev the root and its nested library are merged into one mod file.
#   So this static audit over the five source roots is the only machine check there is.
#
#   P4e motivating case: core/src/main/java/.../data/lang/Translatable.java lived in the
#   same package as the root's three data/lang providers.  While core was an untyped
#   JarJar library it resolved into the unnamed module and nothing complained; the moment
#   core/build.gradle opts into `FMLModType: GAMELIBRARY` core becomes a real module of
#   the GAME layer, and the collision turns into a startup crash.  Fixing one without the
#   other silently converts "NoClassDefFoundError at first use" into "game will not boot".
#
# WHAT IT CHECKS
#   Group every .java file of each root by the package implied by its directory, then
#   report each package that is claimed by more than one root.  A package -- not a class,
#   not a top-level directory -- must be owned by exactly one root.
#
#   core counts like any other root ON PURPOSE.  It is a JarJar-nested library and P4e
#   gave it `FMLModType: GAMELIBRARY`, i.e. a real JPMS module in the GAME layer.  Do NOT
#   add a "core is a library, skip it" exception: that silently re-hides this exact bug
#   (the same silent-escape shape that check-layering.ps1 hit four times when a new root
#   was not added to its root table).
#
# USAGE
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-package-overlap.ps1
#   ... -Verbose    print every package of every root, not just the overlaps
#   Exit code: 0 = no overlap, 1 = at least one package claimed by two roots.
#
# NOTE: this file is deliberately pure ASCII.  Windows PowerShell 5.1 reads a BOM-less
# .ps1 as ANSI, which mangles non-ASCII text and can even swallow quotes mid-script.
# Read-only: it never writes anything.

param(
    [switch]$Verbose
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

# ---------------------------------------------------------------------------
# SOURCE ROOTS -- the single table that knows about roots.  Add a new module
# here the same day its Gradle project appears; a root missing from this table
# makes its files invisible and the audit prints 0 overlaps while exiting 0.
# ---------------------------------------------------------------------------
$roots = [ordered]@{
    'root'          = 'src\main\java'
    'core'          = 'core\src\main\java'
    'coe'           = 'coe\src\main\java'
    'cews'          = 'cews\src\main\java'
    'transmutation' = 'transmutation\src\main\java'
}

# package name -> ordered map of root name -> list of relative file paths
$byPackage = @{}
$filesPerRoot = [ordered]@{}
$pkgsPerRoot = [ordered]@{}

foreach ($rootName in $roots.Keys) {
    $rootPath = Join-Path $repoRoot $roots[$rootName]
    $filesPerRoot[$rootName] = 0
    if (-not (Test-Path -LiteralPath $rootPath)) {
        Write-Host ("MISSING ROOT: {0} -> {1}" -f $rootName, $roots[$rootName])
        continue
    }
    $pkgSet = New-Object 'System.Collections.Generic.HashSet[string]'
    $files = Get-ChildItem -LiteralPath $rootPath -Recurse -File -Filter '*.java'
    foreach ($f in $files) {
        $filesPerRoot[$rootName] = $filesPerRoot[$rootName] + 1
        $rel = $f.FullName.Substring($rootPath.Length).TrimStart('\', '/')
        $dir = Split-Path -Parent $rel
        if ([string]::IsNullOrEmpty($dir)) {
            $pkg = '(default package)'
        } else {
            $pkg = $dir -replace '[\\/]', '.'
        }
        [void]$pkgSet.Add($pkg)
        if (-not $byPackage.ContainsKey($pkg)) {
            $byPackage[$pkg] = [ordered]@{}
        }
        if (-not $byPackage[$pkg].Contains($rootName)) {
            $byPackage[$pkg][$rootName] = New-Object 'System.Collections.Generic.List[string]'
        }
        $byPackage[$pkg][$rootName].Add($rel)
    }
    $pkgsPerRoot[$rootName] = $pkgSet.Count
}

Write-Host 'package-overlap audit: five java source roots, grouped by file directory -> package'
Write-Host ''
foreach ($rootName in $roots.Keys) {
    Write-Host ("  {0,-14} {1,-42} files={2,-5} packages={3}" -f `
        $rootName, $roots[$rootName], $filesPerRoot[$rootName], $pkgsPerRoot[$rootName])
}
Write-Host ''

if ($Verbose) {
    Write-Host 'all packages by root:'
    foreach ($pkg in ($byPackage.Keys | Sort-Object)) {
        $owners = @($byPackage[$pkg].Keys)
        Write-Host ("  {0,-70} [{1}]" -f $pkg, ($owners -join ','))
    }
    Write-Host ''
}

$overlaps = @($byPackage.Keys | Where-Object { @($byPackage[$_].Keys).Count -gt 1 } | Sort-Object)

if ($overlaps.Count -eq 0) {
    Write-Host ("OK: 0 packages are claimed by more than one root ({0} packages total)." -f $byPackage.Count)
    exit 0
}

Write-Host ("VIOLATION: {0} package(s) claimed by more than one root -- JPMS ResolutionException at startup:" -f $overlaps.Count)
foreach ($pkg in $overlaps) {
    $owners = @($byPackage[$pkg].Keys)
    Write-Host ''
    Write-Host ("  package {0}" -f $pkg)
    Write-Host ("    claimed by: {0}" -f ($owners -join ', '))
    foreach ($owner in $owners) {
        foreach ($rel in $byPackage[$pkg][$owner]) {
            Write-Host ("      {0,-14} {1}" -f $owner, $rel)
        }
    }
}
Write-Host ''
Write-Host 'FIX: move the file(s) of one side into a package that side owns exclusively,'
Write-Host '     then re-run this script until it exits 0.'
exit 1
