package com.kanishk.golditemscanner.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.kanishk.golditemscanner.databinding.FragmentScanItemBinding
import kotlinx.coroutines.launch
import java.io.File


class ScanItemFragment : Fragment() {

    private var _binding: FragmentScanItemBinding? = null
    private val CAMERA_REQUEST_CODE = 100
    private lateinit var photoUri: Uri

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val scanItemViewModel = lazy {
        ViewModelProvider(this).get(ScanItemViewModel::class.java)
    }


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scanItemViewModel = scanItemViewModel.value

        _binding = FragmentScanItemBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = scanItemViewModel

        val btnScanItem: Button = binding.btnScanItem

        btnScanItem.setOnClickListener {
            showImageSourceDialog()
        }

        // Observe LiveData fields and update UI elements
        scanItemViewModel.goldRate24K1Tola.observe(viewLifecycleOwner) { goldRate ->
            binding.tvGoldRate24K.text = goldRate
        }

        scanItemViewModel.karat.observe(viewLifecycleOwner) { karat ->
            binding.dropdownArticleKarat.setSelection(if (karat == 24) 0 else 1)
        }

        scanItemViewModel.netWeight.observe(viewLifecycleOwner) { netWeight ->
            binding.tvNetWeight.text = netWeight.toString()
        }

        // Handle makingCharge EditText manually for proper user input detection
        scanItemViewModel.makingCharge.observe(viewLifecycleOwner) { makingChargeValue ->
            if (binding.tvMakingCharge.text.toString() != makingChargeValue.toString()) {
                binding.tvMakingCharge.setText(makingChargeValue.toString())
            }
        }
        
        // Add text watcher for makingCharge to trigger recalculation when user manually edits
        binding.tvMakingCharge.doAfterTextChanged { text ->
            val enteredValue = text.toString().toDoubleOrNull() ?: 0.0
            // Update the ViewModel only if the value actually changed
            if (scanItemViewModel.makingCharge.value != enteredValue) {
                scanItemViewModel.makingCharge.value = enteredValue
                // Don't pass isMakingChargeChanged=true when user manually enters a value
                // We want to use their input, not recalculate it
                scanItemViewModel.recalculateApproxTotalAmount(true)
            }
        }

        scanItemViewModel.luxuryTax.observe(viewLifecycleOwner) { luxuryTax ->
            binding.tvLuxuryTax.text = luxuryTax.toString()
        }

        scanItemViewModel.approxTotalAmount.observe(viewLifecycleOwner) { approxTotalAmount ->
            binding.tvApproxFinalAmount.text = approxTotalAmount.toString()
        }

        // Handle wastage EditText manually for proper user input detection
        scanItemViewModel.wastage.observe(viewLifecycleOwner) { wastageValue ->
            if (binding.tvWastage.text.toString() != wastageValue.toString()) {
                binding.tvWastage.setText(wastageValue.toString())
            }
        }
        
        // Add text watcher for wastage to trigger recalculation when user manually edits
        binding.tvWastage.doAfterTextChanged { text ->
            val enteredValue = text.toString().toDoubleOrNull() ?: 0.0
            // Update the ViewModel only if the value actually changed
            if (scanItemViewModel.wastage.value != enteredValue) {
                scanItemViewModel.wastage.value = enteredValue
                // When wastage changes, we should recalculate making charge since it affects total weight
                scanItemViewModel.recalculateApproxTotalAmount()
            }
        }

        return root
    }



    override fun onResume() {
        super.onResume()
        // Launch a coroutine to call the suspend function
        viewLifecycleOwner.lifecycleScope.launch {
            scanItemViewModel.value.initialLoad(requireActivity().application.baseContext)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Open Camera", "Select from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Choose an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            val photoFile = File.createTempFile("IMG_", ".jpg", requireContext().cacheDir)
            photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            cameraLauncher.launch(cameraIntent)
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, photoUri)
            val rotatedBitmap = handleImageOrientation(photoUri, bitmap)
            handleImageBitmap(rotatedBitmap)
        }
    }

    private fun handleImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val exif = ExifInterface(inputStream!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                handleImageBitmap(bitmap)
            }
        }
    }

    private fun handleImageBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        scanItemViewModel.value.processScannedImage(image)
        // Handle the bitmap (e.g., display it in an ImageView or process it)
        Toast.makeText(requireContext(), "Image captured/selected successfully!", Toast.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}