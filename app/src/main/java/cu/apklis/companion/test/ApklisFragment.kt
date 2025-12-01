package cu.apkuba.test

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.apkuba.sdk.core.Apklis
import cu.apkuba.sdk.utils.Utils
import cu.apkuba.test.databinding.FragmentApklisBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ApklisFragment : Fragment() {

    private var _binding: FragmentApklisBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentApklisBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            launchButton.setOnClickListener {
                binding.packageIdTi.error = null
                val packageId = binding.packageId.text.toString().trim()
                var cancel = false
                var focusView: View? = null
                if (TextUtils.isEmpty(packageId)) {
                    binding.packageIdTi.error = getString(R.string.error_field_required)
                    focusView = binding.packageId
                    cancel = true
                }
                if (!Utils.validatePackageName(packageId)) {
                    binding.packageIdTi.error = getString(R.string.error_package_id)
                    focusView = binding.packageId
                    cancel = true
                }
                if (cancel) {
                    focusView?.requestFocus()
                } else {
                    Utils.openApklisLink(requireContext(), packageId)
                }
            }

            validateButton.setOnClickListener {
                binding.packageIdTi.error = null
                val packageId = binding.packageId.text.toString().trim()
                var cancel = false
                var focusView: View? = null
                if (TextUtils.isEmpty(packageId)) {
                    binding.packageIdTi.error = getString(R.string.error_field_required)
                    focusView = binding.packageId
                    cancel = true
                }
                if (!Utils.validatePackageName(packageId)) {
                    binding.packageIdTi.error = getString(R.string.error_package_id)
                    focusView = binding.packageId
                    cancel = true
                }
                if (cancel) {
                    focusView?.requestFocus()
                } else {
                    lifecycleScope.launch {
                        val result = Apklis.isPurchased(requireContext(), packageId)
                        MaterialAlertDialogBuilder(requireContext())
                            .setIcon(R.drawable.info_24px)
                            .setTitle(getString(R.string.result))
                            .setMessage(
                                result.toString()
                            )
                            .setCancelable(false)
                            .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                                dialog.dismiss()
                            }

                            .show()

                        Log.i("Apklis", "Result: $result")
                    }

                }

            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}