package seamcarving

import java.awt.Color
import java.awt.Color.red
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt


fun main(args: Array<String>) {
    var inputFilePath = ""
    var outFilePath = ""
    var width = 0
    var height = 0
    for (i in args.indices) {
        when(args[i]){
            "-in" ->  inputFilePath = args[i + 1]
            "-out" -> outFilePath = args[i + 1]
            "-width" -> width = args[i + 1].toInt()
            "-height" -> height = args[i + 1].toInt()
        }
    }
    val image1 = ImageIO.read(File(inputFilePath))
    val factor = 0.41
//    val image = transposeImage(image1, 90.0)
//    val imageEnergy = getImageEnergy(image)
//    val imageEnergySum = getEnergySumm(imageEnergy)
//    val seam = getSeam(imageEnergySum)
//
//    for (j in 0 until image.height) {
//        image.setRGB(seam[j], j, red.rgb)
//    }
//    val image2 = transposeImage(image, -90.0)
    val image2 = resizeImage(image1, width, height)
    ImageIO.write(image2, "png", File(outFilePath))
}

fun getSeam(imageEnergySum: Array<Array<Double>>): Array<Int> {
    val result = Array (imageEnergySum[0].size) { 0 }
    val lastY = imageEnergySum[0].size - 1
    var minEnergy = imageEnergySum[0][lastY]
    //Find min energy in last row
    for (i in imageEnergySum.indices) {
        if (imageEnergySum[i][lastY] < minEnergy) {
            minEnergy = imageEnergySum[i][lastY]
            result[lastY] = i
        }
    }
    //Find X indicies of Seam
    for (yIndex in lastY-1 downTo 0) {
        val prevXIndex = result[yIndex + 1]
        result[yIndex] = prevXIndex
        if (prevXIndex > 0 && imageEnergySum[prevXIndex][yIndex] > imageEnergySum[prevXIndex - 1][yIndex]) result[yIndex] = prevXIndex - 1
        if (prevXIndex < imageEnergySum.size - 1 && imageEnergySum[result[yIndex]][yIndex] > imageEnergySum[prevXIndex + 1][yIndex]) result[yIndex] = prevXIndex + 1
    }
    return result
}

fun getEnergySumm (imageEnergy: Array<Array<Double>>):Array<Array<Double>> {
    val imageEnergySumm = Array(imageEnergy.size) { Array(imageEnergy[0].size) {0.0} }
    //Set sum of energy in first row = energy
    for (i in imageEnergy.indices) {
        imageEnergySumm[i][0] = imageEnergy[i][0]
    }
    //Calculate minimal sum of energy to other pixels
    for (j in 1 until imageEnergy[0].size){
        for (i in imageEnergy.indices){
            imageEnergySumm[i][j] = imageEnergySumm[i][j - 1]
            if (i > 0 && imageEnergySumm[i - 1][j - 1] < imageEnergySumm[i][j]) imageEnergySumm[i][j] = imageEnergySumm[i - 1][j - 1]
            if (i < imageEnergy.size - 1 && imageEnergySumm[i + 1][j - 1] < imageEnergySumm[i][j]) imageEnergySumm[i][j] = imageEnergySumm[i + 1][j - 1]
            imageEnergySumm[i][j] += imageEnergy[i][j]
        }
    }
    return imageEnergySumm
}

fun getImageEnergy (image: BufferedImage): Array<Array<Double>> {

    fun energyCalc(xminus: Color, xplus: Color, yminus: Color, yplus: Color): Double {
        val x_differenceR = (xminus.red - xplus.red).toDouble()
        val x_differenceG = (xminus.green - xplus.green).toDouble()
        val x_differenceB = (xminus.blue - xplus.blue).toDouble()
        val x_gradient = x_differenceR.pow(2.0) + x_differenceG.pow(2.0) + x_differenceB.pow(2.0)
        val y_differenceR = (yminus.red - yplus.red).toDouble()
        val y_differenceG = (yminus.green - yplus.green).toDouble()
        val y_differenceB = (yminus.blue - yplus.blue).toDouble()
        val y_gradient = y_differenceR.pow(2.0) + y_differenceG.pow(2.0) + y_differenceB.pow(2.0)
        return sqrt(x_gradient + y_gradient)
    }

    var xIndex: Int
    var yIndex: Int
    val energy: Array<Array<Double>> = Array(image.width) { Array(image.height) {0.0} }
    for (i in 0 until image.width) {
        xIndex = when (i) {
            0 -> 1
            image.width - 1 -> image.width - 2
            else -> i
        }
        for (j in 0 until image.height) {
            yIndex = when (j) {
                0 -> 1
                image.height - 1 -> image.height - 2
                else -> j
            }
            val xminus = Color(image.getRGB(xIndex - 1, j))
            val xplus = Color(image.getRGB(xIndex + 1, j))
            val yminus = Color(image.getRGB(i, yIndex - 1))
            val yplus = Color(image.getRGB(i, yIndex + 1))
            energy[i][j] = energyCalc(xminus, xplus, yminus, yplus)
        }
    }
    return energy
}

fun transposeImage(image: BufferedImage, ang: Double): BufferedImage{
    val angle = Math.toRadians(ang)
    val w = image.width
    val h = image.height
    val rotatedImage = BufferedImage(h, w, image.type)
    val at = AffineTransform()
    at.translate(h.toDouble()/2, w.toDouble()/2)
    at.rotate(angle, 0.0, 0.0)
    at.translate(-w.toDouble()/2, -h.toDouble()/2)
    val rotateOp = AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
    rotateOp.filter(image, rotatedImage)
    return rotatedImage
}

fun resizeImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage? {
    val nWidth = originalImage.width - targetWidth
    val nHeight = originalImage.height - targetHeight
    val resizedImage = BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_RGB)
    val graphics2D = resizedImage.createGraphics()
    graphics2D.drawImage(originalImage, 0, 0, nWidth, nHeight, null)
    graphics2D.dispose()
    return resizedImage
}