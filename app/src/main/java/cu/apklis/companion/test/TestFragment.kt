package cu.apkuba.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import cu.apkuba.test.databinding.FragmentTestBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class TestFragment : Fragment() {

    private var _binding: FragmentTestBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var pageAdapter: PageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLicenseAdapter()

    }


    inner class PageAdapter(
        fragment: Fragment?
    ) :
        FragmentStateAdapter(fragment!!) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {

            return when (position) {

                0 -> ApKubaFragment()
                else -> ApklisFragment()
            }
        }

    }

    private fun setupLicenseAdapter() {
        // Configurar el adaptador
        pageAdapter =
            PageAdapter(this)
        binding.viewPager.adapter = pageAdapter
        // Deshabilitar el deslizamiento del ViewPager2 si es necesario
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(
            binding.tabLayout, binding.viewPager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "ApKuba"
                else -> tab.text = "Apklis"
            }


        }.attach()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}