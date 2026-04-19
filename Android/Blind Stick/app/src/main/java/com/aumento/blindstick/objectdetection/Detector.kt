package com.aumento.blindstick.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Detector
 *
 * Core TensorFlow Lite detection engine.
 *
 * Responsibilities:
 * - Loads TFLite model and label file
 * - Preprocesses camera frames
 * - Runs inference
 * - Converts raw model output into BoundingBox objects
 * - Applies Non-Max Suppression (NMS)
 * - Sends final results back through DetectorListener
 */
class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener
) {

    // TensorFlow Lite interpreter instance (runs the ML model)
    private var interpreter: Interpreter? = null

    // List of class labels loaded from label file
    private var labels = mutableListOf<String>()

    // Model input width (derived from model input tensor shape)
    private var tensorWidth = 0

    // Model input height (derived from model input tensor shape)
    private var tensorHeight = 0

    // Number of output channels (bbox + class scores)
    private var numChannel = 0

    // Number of detection candidates predicted by the model
    private var numElements = 0

    // Image preprocessing pipeline:
    // - Normalizes pixel values
    // - Casts image to required data type (FLOAT32)
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    /**
     * setup()
     * Loads model, initializes interpreter,
     * reads input/output tensor shapes,
     * and loads label file from assets.
     */
    fun setup() {
        // Load model file from assets as memory-mapped file
        val model = FileUtil.loadMappedFile(context, modelPath)
        // Use 4 CPU threads for faster inference
        val options = Interpreter.Options()
        // Use 4 CPU threads for faster inference
        options.numThreads = 4
        interpreter = Interpreter(model, options)

        // Read model input tensor shape [1, height, width, channels]
        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        // Read model output tensor shape [1, numChannel, numElements]
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]

        try {
            // Load label file from assets (each line = class name)
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * clear()
     * Releases interpreter resources to prevent memory leaks.
     */
    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    /**
     * detect()
     * Main detection pipeline:
     * - Validates model state
     * - Resizes input frame
     * - Preprocesses image
     * - Runs inference
     * - Converts output to bounding boxes
     * - Notifies listener
     */
    fun detect(frame: Bitmap) {
        interpreter ?: return
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return

        // Record start time for performance measurement
        var inferenceTime = SystemClock.uptimeMillis()

        // Resize frame to model-required input size
        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        // Convert Bitmap into TensorImage (TFLite input format)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        // Apply normalization and casting operations
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        // Allocate output tensor buffer to receive model predictions
        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)

        // Convert raw float output into filtered BoundingBox list
        val bestBoxes = bestBox(output.floatArray)
        // Calculate total inference time
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime


        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        }

        detectorListener.onDetect(bestBoxes, inferenceTime)
    }

    /**
     * bestBox()
     * Parses raw model output:
     * - Finds highest confidence class per candidate
     * - Converts center-based boxes to corner format
     * - Filters by confidence threshold
     * - Applies NMS
     */
    private fun bestBox(array: FloatArray) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        // Iterate through each predicted detection candidate
        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            // Filter out weak detections using confidence threshold
            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                // Store valid bounding box
                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    /**
     * applyNMS()
     * Removes overlapping boxes using
     * Intersection over Union (IoU) threshold.
     */
    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        // Sort boxes by confidence (highest first)
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    /**
     * calculateIoU()
     * Computes Intersection over Union between two boxes.
     * Used for overlap suppression.
     */
    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    // Callback interface used to send detection results to UI layer
    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    /**
     * Model configuration constants
     * - Normalization values
     * - Tensor data types
     * - Confidence & IoU thresholds
     */
    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.5F
    }
}