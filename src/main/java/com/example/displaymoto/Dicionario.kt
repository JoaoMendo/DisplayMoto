package com.example.displaymoto

// Aqui podes adicionar centenas de variáveis no futuro sem estragar o resto da app!
data class AppStrings(
    val settingsTitle: String, val back: String,
    val bluetoothTitle: String, val bluetoothDesc: String,
    val connectTitle: String, val connectDesc: String,
    val colorTitle: String, val colorDesc: String, val more: String,
    val langTitle: String, val langDesc: String,
    val persTitle: String, val persDesc: String,
    val aboutTitle: String, val aboutDesc: String,
    val colorLibraryTitle: String, val dragColor: String,
    val select: String, val cancel: String,
    val road3d: String, val warning: String, val lowBattery: String,
    val critRange1: String, val critRange2: String
    // Futuro: val gpsTitle: String, val mediaTitle: String, etc...
)

enum class AppLanguage(val displayName: String) {
    EN("ENGLISH"),
    PT("PORTUGUÊS"),
    ES("ESPAÑOL")
}

fun getAppStrings(lang: AppLanguage): AppStrings {
    return when (lang) {
        AppLanguage.EN -> AppStrings(
            "SETTINGS", "BACK",
            "BLUETOOTH", "Manage phone and intercom connections",
            "CONNECT MYFULGORA", "Sync your motorcycle with the official app",
            "COLOUR", "Tap for full library or pick a shortcut", "MORE+",
            "LANGUAGE", "Select the system display language",
            "PERSONALIZATION", "Adjust display, units and layout preferences",
            "ABOUT THE MOTORCYCLE", "System information, software updates and details",
            "COLOUR LIBRARY", "Drag to pick any colour", "SELECT", "CANCEL",
            "3D ROAD", "W A R N I N G", "LOW BATTERY (20%)",
            "Critical range. Please proceed to", "a charging station immediately."
        )
        AppLanguage.PT -> AppStrings(
            "DEFINIÇÕES", "VOLTAR",
            "BLUETOOTH", "Gerir telemóvel e intercomunicadores",
            "CONECTAR MYFULGORA", "Sincronize a sua mota com a app oficial",
            "COR", "Toque para a biblioteca ou escolha um atalho", "MAIS+",
            "IDIOMA", "Selecione o idioma de exibição do sistema",
            "PERSONALIZAÇÃO", "Ajustar ecrã, unidades e preferências",
            "SOBRE A MOTA", "Informações do sistema, atualizações e detalhes",
            "BIBLIOTECA DE CORES", "Arraste para escolher uma cor", "SELECIONAR", "CANCELAR",
            "ESTRADA 3D", "A V I S O", "BATERIA FRACA (20%)",
            "Autonomia crítica. Por favor, dirija-se", "a um posto de carregamento imediatamente."
        )
        AppLanguage.ES -> AppStrings(
            "AJUSTES", "VOLVER",
            "BLUETOOTH", "Gestionar teléfono e intercomunicadores",
            "CONECTAR MYFULGORA", "Sincroniza tu moto con la app oficial",
            "COLOR", "Toca para la biblioteca o elige un atajo", "MÁS+",
            "IDIOMA", "Selecciona el idioma de la pantalla",
            "PERSONALIZACIÓN", "Ajustar pantalla, unidades y preferencias",
            "SOBRE LA MOTO", "Información del sistema y detalles",
            "BIBLIOTECA DE COLORES", "Arrastra para elegir un color", "SELECCIONAR", "CANCELAR",
            "CARRETERA 3D", "A V I S O", "BATERÍA BAJA (20%)",
            "Autonomía crítica. Por favor, diríjase", "a una estación de carga inmediatamente."
        )
    }
}