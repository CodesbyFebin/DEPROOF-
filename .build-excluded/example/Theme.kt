package com.example
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
object Palette {
    val obsidian=Color(0xFF070B14);val raised=Color(0xFF101722);val ink=Color(0xFFE8EEF8);val muted=Color(0xFF93A0B8)
    val mint=Color(0xFF3DDC97);val cyan=Color(0xFF22D3EE);val blue=Color(0xFF3B82F6);val purple=Color(0xFF8B5CF6)
    val danger=Color(0xFFF07178);val warning=Color(0xFFE7C36A);val line=Color(0xFF1E2A3D);val buttonLabel=Color(0xFF062018)
}
fun deproofColors(pearl: Boolean) = if(pearl) lightColorScheme(primary=Color(0xFF167B56),onPrimary=Color.White,background=Color(0xFFF7F4EF),surface=Color.White,onSurface=Color(0xFF1A1714),onBackground=Color(0xFF1A1714),onSurfaceVariant=Color(0xFF555166),outline=Color(0xFF797286),error=Color(0xFFB42332)) else darkColorScheme(primary=Palette.mint,onPrimary=Palette.buttonLabel,background=Palette.obsidian,surface=Palette.raised,onSurface=Palette.ink,onBackground=Palette.ink,onSurfaceVariant=Palette.muted,error=Palette.danger,outline=Palette.muted)
