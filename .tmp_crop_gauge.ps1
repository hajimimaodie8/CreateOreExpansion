Add-Type -AssemblyName System.Drawing

# ASCII-only script (PS 5.1 reads non-BOM .ps1 as ANSI, so no CJK literal is allowed here).
# The source directory name contains CJK characters, so it is resolved by wildcard instead.

$srcItem = Get-Item "E:\mc\*\mod*\wave_query_gauge.png"
$src = $srcItem.FullName

$dstDir = "E:\mc\mcmod\createoreexpansion\src\main\resources\assets\createoreexpansion\textures\item"
if (-not (Test-Path $dstDir)) {
    New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
}

$srcImg = [System.Drawing.Image]::FromFile($src)
Write-Output ("SOURCE-LEAF        = {0} ({1} bytes)" -f $srcItem.Name, $srcItem.Length)
Write-Output ("SOURCE-SIZE        = {0}x{1}" -f $srcImg.Width, $srcImg.Height)
Write-Output ("SOURCE-PIXELFORMAT = {0}" -f $srcImg.PixelFormat)

$frameCount = 8
$frameH = [int]($srcImg.Height / $frameCount)
Write-Output ("FRAME-GEOMETRY     = {0} frames of {1}x{2}" -f $frameCount, $srcImg.Width, $frameH)

# Exact crop (no scaling / no blending): Clone(rect, same pixel format)
$rect = New-Object System.Drawing.Rectangle(0, 0, $srcImg.Width, $frameH)
$idleImg = $srcImg.Clone($rect, $srcImg.PixelFormat)

$idlePath = Join-Path $dstDir "wave_query_gauge_idle.png"
$idleImg.Save($idlePath, [System.Drawing.Imaging.ImageFormat]::Png)

# Pixel-level proof: idle(x,y) must equal source(x, y) for the whole first frame
$mismatch = 0
for ($y = 0; $y -lt $frameH; $y++) {
    for ($x = 0; $x -lt $srcImg.Width; $x++) {
        if ($idleImg.GetPixel($x, $y).ToArgb() -ne $srcImg.GetPixel($x, $y).ToArgb()) { $mismatch++ }
    }
}
Write-Output ("IDLE-VS-SOURCE     = {0} mismatching pixels out of {1}" -f $mismatch, ($frameH * $srcImg.Width))

# Copy the animated strip + its mcmeta (name must stay <texture>.png.mcmeta, beside the png)
$animPath = Join-Path $dstDir "wave_query_gauge.png"
Copy-Item $src $animPath -Force
$mcmetaSrc = "$src.mcmeta"
$mcmetaDst = Join-Path $dstDir "wave_query_gauge.png.mcmeta"
if (Test-Path $mcmetaSrc) {
    Copy-Item $mcmetaSrc $mcmetaDst -Force
    Write-Output ("MCMETA             = copied to {0} ({1} bytes)" -f (Split-Path $mcmetaDst -Leaf), (Get-Item $mcmetaDst).Length)
} else {
    Write-Output "MCMETA             = !! MISSING at source"
}

$idleImg.Dispose()
$srcImg.Dispose()

foreach ($leaf in @("wave_query_gauge.png", "wave_query_gauge.png.mcmeta", "wave_query_gauge_idle.png")) {
    $p = Join-Path $dstDir $leaf
    if (-not (Test-Path $p)) { Write-Output ("DST {0} = MISSING" -f $leaf); continue }
    if ($leaf.EndsWith(".png")) {
        $img = [System.Drawing.Image]::FromFile($p)
        Write-Output ("DST {0} = {1}x{2}, {3} bytes" -f $leaf, $img.Width, $img.Height, (Get-Item $p).Length)
        $img.Dispose()
    } else {
        Write-Output ("DST {0} = {1} bytes" -f $leaf, (Get-Item $p).Length)
    }
}
