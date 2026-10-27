package it.edo.codecprobe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Separate UI lab entry point; the legacy activities and their coordinators remain available. */
internal class ComposeDesignPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDesignPrototype(
                onExit = ::finish,
                onOpenPhoto = { startActivity(Intent(this, ImageActivity::class.java)) }
            )
        }
    }
}
