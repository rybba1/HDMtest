package com.example.hdm.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class SafetyTip(
    val id: Int,
    val title: String,
    val message: String,
    val icon: ImageVector,
    val category: SafetyCategory
)

enum class SafetyCategory {
    FORKLIFT,    // Wózki widłowe / ruch magazynowy
    HEIGHT,      // Praca na wysokości / składowanie
    ERGONOMICS,  // Ergonomia / praca ręczna
    EVACUATION,  // Ewakuacja / pożar
    FIRST_AID,   // Pierwsza pomoc
    GENERAL      // Ogólne zasady BHP / organizacja pracy
}

object SafetyTips {
    private val tips = listOf(

        // --- FORKLIFT (Wózki widłowe, ruch magazynowy) ---

        SafetyTip(
            id = 1,
            title = "Odstęp bezpieczeństwa",
            message = "Zachowuj co najmniej 3 sekundy odstępu za innym wózkiem. Masz wtedy czas na reakcję przy nagłym hamowaniu.",
            icon = Icons.Default.LocalShipping,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 2,
            title = "Kontrola przed pracą",
            message = "Przed rozpoczęciem zmiany sprawdź hamulce, sygnały dźwiękowe, oświetlenie, stan wideł i opon. Wózek z usterką nie może być użytkowany.",
            icon = Icons.Default.Build,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 3,
            title = "Prędkość robocza",
            message = "W alejkach magazynowych poruszaj się z prędkością zbliżoną do prędkości pieszego. Zbyt duża prędkość to jedna z głównych przyczyn kolizji.",
            icon = Icons.Default.Speed,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 4,
            title = "Widoczność ładunku",
            message = "Jeżeli ładunek ogranicza Ci widoczność – jedź tyłem. Operator musi zawsze widzieć kierunek jazdy.",
            icon = Icons.Default.Visibility,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 5,
            title = "Strefy pieszych",
            message = "W strefach pieszych zawsze ustępuj pierwszeństwa pracownikom poruszającym się pieszo. Klakson nie zwalnia z obowiązku zatrzymania się.",
            icon = Icons.Default.DirectionsRun,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 6,
            title = "Cofanie wózkiem",
            message = "Podczas cofania odwróć głowę i obserwuj tor jazdy. Lustra i kamery są wsparciem, ale nie zastępują bezpośredniej obserwacji.",
            icon = Icons.Default.Sync,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 7,
            title = "Transport ładunku",
            message = "Przewoź ładunek możliwie nisko nad podłożem, ze słupem masztu lekko odchylonym do tyłu. To poprawia stabilność.",
            icon = Icons.Default.KeyboardArrowDown,
            category = SafetyCategory.FORKLIFT
        ),
        SafetyTip(
            id = 8,
            title = "Ładowanie akumulatorów",
            message = "Ładuj wózki elektryczne wyłącznie w wyznaczonych strefach. Zapewnij wentylację i bezwzględnie przestrzegaj zakazu palenia (wydziela się wodór).",
            icon = Icons.Default.PropaneTank,
            category = SafetyCategory.FORKLIFT
        ),

        // --- HEIGHT (Składowanie wysokie / regały / upadki z wysokości) ---

        SafetyTip(
            id = 9,
            title = "Stabilność palety",
            message = "Nie odkładaj na regał palet uszkodzonych, przechylonych lub z towarem wystającym poza obrys. Taki ładunek może spaść.",
            icon = Icons.Default.Inventory,
            category = SafetyCategory.HEIGHT
        ),
        SafetyTip(
            id = 10,
            title = "Nośność regałów",
            message = "Nie przekraczaj dopuszczalnego obciążenia półki. Jeśli tabliczka nośności jest nieczytelna lub brak oznaczenia – nie składaj tam towaru i zgłoś to.",
            icon = Icons.Default.Report,
            category = SafetyCategory.HEIGHT
        ),
        SafetyTip(
            id = 11,
            title = "Uszkodzenia konstrukcji",
            message = "Widzisz pęknięty słup regału, wygięty trawers lub ślady uderzenia widłami? Natychmiast oznacz miejsce i zgłoś do przełożonego / BHP. Nie składować do czasu oceny.",
            icon = Icons.Default.Warning,
            category = SafetyCategory.HEIGHT
        ),
        SafetyTip(
            id = 12,
            title = "Środki ochrony indywidualnej",
            message = "W strefie wysokiego składowania noś kask ochronny oraz obuwie z noskiem ochronnym. Chronisz głowę i stopy przed spadającymi elementami.",
            icon = Icons.Default.Construction,
            category = SafetyCategory.HEIGHT
        ),
        SafetyTip(
            id = 13,
            title = "Praca na wysokości",
            message = "Nie wolno wspinać się na regały ani stać na widłach 'na chwilę'. Do pracy na wysokości używaj tylko dopuszczonych podestów i koszy roboczych.",
            icon = Icons.Default.Block,
            category = SafetyCategory.HEIGHT
        ),
        SafetyTip(
            id = 14,
            title = "Centrowanie palety",
            message = "Paleta powinna spoczywać centralnie na trawersach, w pełnym oparciu. Paleta wysunięta poza krawędź regału to zagrożenie upadkiem towaru.",
            icon = Icons.Default.GridOn,
            category = SafetyCategory.HEIGHT
        ),

        // --- ERGONOMICS (Ergonomia pracy ręcznej, obciążenia fizyczne) ---

        SafetyTip(
            id = 15,
            title = "Podnoszenie ładunku",
            message = "Uginaj kolana, trzymaj plecy możliwie prosto i trzymaj ładunek blisko ciała. Unikaj szarpania oraz pochylania się w pasie przy prostych nogach.",
            icon = Icons.Default.FitnessCenter,
            category = SafetyCategory.ERGONOMICS
        ),
        SafetyTip(
            id = 16,
            title = "Skręcanie z ładunkiem",
            message = "Nie skręcaj tułowia z ciężarem w rękach. Jeśli musisz się obrócić – przestaw całe ciało wraz ze stopami.",
            icon = Icons.Default.AccessibilityNew,
            category = SafetyCategory.ERGONOMICS
        ),
        SafetyTip(
            id = 17,
            title = "Praca stojąca",
            message = "Przy pracy stojącej zmieniaj pozycję co pewien czas, przenoś ciężar z nogi na nogę i rób krótkie przerwy od obciążenia.",
            icon = Icons.Default.DirectionsWalk,
            category = SafetyCategory.ERGONOMICS
        ),
        SafetyTip(
            id = 18,
            title = "Dopuszczalne masy",
            message = "Szanuj limity dźwigania. Nadmierne obciążenie jednorazowe lub powtarzalne jest częstą przyczyną urazów kręgosłupa i barków.",
            icon = Icons.Default.MonitorWeight,
            category = SafetyCategory.ERGONOMICS
        ),
        SafetyTip(
            id = 19,
            title = "Zmęczenie i ból",
            message = "Ból pleców, nadgarstków lub barków zgłaszaj natychmiast przełożonemu. Praca mimo bólu może doprowadzić do trwałego urazu.",
            icon = Icons.Default.HealthAndSafety,
            category = SafetyCategory.ERGONOMICS
        ),
        SafetyTip(
            id = 20,
            title = "Praca w upale",
            message = "W wysokiej temperaturze pij wodę regularnie małymi porcjami. Odwodnienie obniża koncentrację i zwiększa ryzyko zasłabnięcia.",
            icon = Icons.Default.WaterDrop,
            category = SafetyCategory.ERGONOMICS
        ),

        // --- EVACUATION (Ewakuacja, pożar, alarm) ---

        SafetyTip(
            id = 21,
            title = "Znajomość wyjść",
            message = "Zawsze wiedz, gdzie znajduje się najbliższe wyjście ewakuacyjne oraz droga alternatywna. W zadymieniu możesz nie mieć czasu na szukanie.",
            icon = Icons.Default.ExitToApp,
            category = SafetyCategory.EVACUATION
        ),
        SafetyTip(
            id = 22,
            title = "Nie blokuj dróg ewakuacyjnych",
            message = "Nie ustawiaj palet, pojemników ani odpadów na drogach ewakuacyjnych. Niedrożna droga ewakuacji to bezpośrednie zagrożenie życia.",
            icon = Icons.Default.Block,
            category = SafetyCategory.EVACUATION
        ),
        SafetyTip(
            id = 23,
            title = "Reakcja na alarm",
            message = "Po ogłoszeniu alarmu przerwij pracę i udaj się do wyjścia ewakuacyjnego. Nie wracaj po prywatne rzeczy.",
            icon = Icons.Default.LocalFireDepartment,
            category = SafetyCategory.EVACUATION
        ),
        SafetyTip(
            id = 24,
            title = "Wczesne wykrycie zagrożenia",
            message = "Zapach spalenizny, zadymienie lub przegrzany przewód to sygnały alarmowe. Natychmiast zgłoś to i uruchom procedurę alarmową.",
            icon = Icons.Default.Warning,
            category = SafetyCategory.EVACUATION
        ),
        SafetyTip(
            id = 25,
            title = "Gaśnice",
            message = "Gaś małe źródła ognia tylko wtedy, gdy nie narażasz swojego zdrowia. Najpierw alarmuj, dopiero potem reaguj gaśnicą.",
            icon = Icons.Default.FireExtinguisher,
            category = SafetyCategory.EVACUATION
        ),

        // --- FIRST_AID (Pierwsza pomoc) ---

        SafetyTip(
            id = 26,
            title = "Ocena stanu poszkodowanego",
            message = "Podejdź bezpiecznie, sprawdź przytomność ('Czy mnie słyszysz?'), sprawdź oddech przez ok. 10 sekund. Oceń sytuację zanim podejmiesz działanie.",
            icon = Icons.Default.HealthAndSafety,
            category = SafetyCategory.FIRST_AID
        ),
        SafetyTip(
            id = 27,
            title = "Pozycja bezpieczna",
            message = "Osoba nieprzytomna, ale oddychająca powinna zostać ułożona w pozycji bocznej bezpiecznej. Chronisz ją przed zadławieniem.",
            icon = Icons.Default.AirlineSeatReclineNormal,
            category = SafetyCategory.FIRST_AID
        ),
        SafetyTip(
            id = 28,
            title = "RKO",
            message = "Brak oddechu? Wezwij 112 i rozpocznij resuscytację krążeniowo-oddechową: 30 uciśnięć klatki piersiowej, 2 oddechy ratownicze, tempo ok. 100–120/min.",
            icon = Icons.Default.MonitorHeart,
            category = SafetyCategory.FIRST_AID
        ),
        SafetyTip(
            id = 29,
            title = "Krwawienie",
            message = "Przy krwotoku uciskaj ranę czystym opatrunkiem bez przerw. Nie zdejmuj opatrunku co chwilę, żeby 'sprawdzić'.",
            icon = Icons.Default.Bloodtype,
            category = SafetyCategory.FIRST_AID
        ),
        SafetyTip(
            id = 30,
            title = "Substancje chemiczne w oku",
            message = "Jeśli substancja chemiczna dostała się do oka, rozpocznij natychmiastowe płukanie ciągłym strumieniem wody przez minimum 15 minut i wezwij pomoc.",
            icon = Icons.Default.RemoveRedEye,
            category = SafetyCategory.FIRST_AID
        ),

        // --- GENERAL (Organizacja pracy, kultura bezpieczeństwa) ---

        SafetyTip(
            id = 31,
            title = "Porządek na stanowisku",
            message = "Utrzymuj czystość i porządek w miejscu pracy. Bałagan zwiększa ryzyko potknięć, kolizji i uszkodzeń towaru.",
            icon = Icons.Default.CleaningServices,
            category = SafetyCategory.GENERAL
        ),
        SafetyTip(
            id = 32,
            title = "Narzędzia tnące",
            message = "Używaj wyłącznie nożyków bezpiecznych z chowanym ostrzem. Zawsze tnij ruchem 'od siebie', a drugą rękę trzymaj poza linią cięcia.",
            icon = Icons.Default.ContentCut,
            category = SafetyCategory.GENERAL
        ),
        SafetyTip(
            id = 33,
            title = "Zgłaszanie zagrożeń",
            message = "Masz obowiązek zgłaszać zauważone nieprawidłowości (uszkodzony sprzęt, wycieki, blokady wyjść ewakuacyjnych). To element kultury bezpieczeństwa.",
            icon = Icons.Default.ReportProblem,
            category = SafetyCategory.GENERAL
        ),
        SafetyTip(
            id = 34,
            title = "Rozlane płyny",
            message = "Plama oleju lub innego płynu na posadzce to ryzyko poślizgnięcia. Zabezpiecz miejsce i użyj sorbentu lub zgłoś osobie odpowiedzialnej.",
            icon = Icons.Default.CleaningServices,
            category = SafetyCategory.GENERAL
        ),
        SafetyTip(
            id = 35,
            title = "Komunikacja",
            message = "Informuj współpracowników o manewrach wózka, nietypowych pracach lub zagrożeniach. Jasna komunikacja ogranicza ryzyko wypadków.",
            icon = Icons.Default.Chat,
            category = SafetyCategory.GENERAL
        )
    )

    fun getRandomTip(): SafetyTip = tips.random()

    fun getTipsByCategory(category: SafetyCategory): List<SafetyTip> {
        return tips.filter { it.category == category }
    }

    fun getAllTips(): List<SafetyTip> = tips
}
