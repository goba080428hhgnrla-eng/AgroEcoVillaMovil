package com.example.agroconectavilla

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RepartidoresFragment : Fragment() {

    private var usuarioId: Int = 1

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etAutomovil: EditText
    private lateinit var btnEnviar: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_repartidores,
            container,
            false
        )

        // RECIBIR ID
        usuarioId = arguments?.getInt("usuarioId") ?: 1

        // VISTAS
        etNombre = view.findViewById(R.id.etNombre)
        etCorreo = view.findViewById(R.id.etCorreo)
        etTelefono = view.findViewById(R.id.etTelefono)
        etAutomovil = view.findViewById(R.id.etAutomovil)
        btnEnviar = view.findViewById(R.id.btnEnviar)

        // BLOQUEAR CAMPOS
        bloquearCampo(etNombre)
        bloquearCampo(etCorreo)
        bloquearCampo(etTelefono)

        // CARGAR DATOS
        cargarDatosUsuario()

        // BOTÓN ACTUALIZAR AUTOMÓVIL
        btnEnviar.setOnClickListener {

            val automovil = etAutomovil.text.toString()

            if (automovil.isEmpty()) {

                etAutomovil.error = "Ingresa tu automóvil"

            } else {

                actualizarAutomovil(automovil)
            }
        }

        return view
    }

    // BLOQUEAR CAMPOS
    private fun bloquearCampo(editText: EditText) {
        editText.isFocusable = false
        editText.isClickable = false
        editText.isCursorVisible = false
    }

    // TRAER DATOS DESDE AURORA POSTGRESQL (VÍA API)
    private fun cargarDatosUsuario() {

        val url = "https://TU_API/cliente/$usuarioId"
        //val url = "https://jsonplaceholder.typicode.com/users/1"

        val request = StringRequest(
            Request.Method.GET,
            url,

            { response ->

                try {

                    val json = JSONObject(response)

                    etNombre.setText(json.getString("nombre"))
                    etCorreo.setText(json.getString("correo"))
                    etTelefono.setText(json.getString("telefono"))
                    etAutomovil.setText(json.getString("automovil"))

                } catch (e: Exception) {

                    Toast.makeText(
                        requireContext(),
                        "Error al procesar datos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            {
                Toast.makeText(
                    requireContext(),
                    "Error de conexión con servidor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ACTUALIZAR SOLO AUTOMÓVIL
    private fun actualizarAutomovil(automovil: String) {

        val url = "https://TU_API/cliente/actualizar"

        val request = object : StringRequest(
            Method.POST,
            url,

            {

                Toast.makeText(
                    requireContext(),
                    "Automóvil actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            },

            {

                Toast.makeText(
                    requireContext(),
                    "Error al actualizar",
                    Toast.LENGTH_SHORT
                ).show()
            }

        ) {
            override fun getParams(): MutableMap<String, String> {

                return hashMapOf(
                    "id" to usuarioId.toString(),
                    "automovil" to automovil
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // NEW INSTANCE
    companion object {

        fun newInstance(usuarioId: Int): RepartidoresFragment {

            val fragment = RepartidoresFragment()

            val bundle = Bundle()
            bundle.putInt("usuarioId", usuarioId)

            fragment.arguments = bundle

            return fragment
        }
    }
}