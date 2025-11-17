package com.example.hdm.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class SafetyQuiz(
    val id: Int,
    val question: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val icon: ImageVector,
    val category: SafetyCategory
)

object SafetyQuizzes {
    private val quizzes = listOf(

        // --- WÓZKI WIDŁOWE / RUCH W MAGAZYNIE ---

        SafetyQuiz(
            id = 1,
            question = "Jak należy zachować się podczas jazdy wózkiem widłowym w strefie pieszych?",
            answers = listOf(
                "Zachować szczególną ostrożność i ustąpić pierwszeństwa pieszym",
                "Użyć klaksonu i kontynuować jazdę bez zmiany prędkości",
                "Przejechać szybciej, aby jak najszybciej opuścić strefę"
            ),
            correctAnswerIndex = 0,
            explanation = "Wózek ma zawsze obowiązek ustąpienia pierwszeństwa pieszemu. Prędkość należy dostosować do warunków na trasie.",
            icon = Icons.Default.DirectionsRun,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 2,
            question = "Kiedy należy wykonać kontrolę stanu technicznego wózka widłowego (hamulce, widły, sygnały dźwiękowe)?",
            answers = listOf(
                "Raz w tygodniu",
                "Tylko jeśli zauważysz usterkę",
                "Każdorazowo przed rozpoczęciem pracy"
            ),
            correctAnswerIndex = 2,
            explanation = "Przed rozpoczęciem każdej zmiany operator ma obowiązek sprawdzić stan techniczny wózka. Sprzęt niesprawny nie może być dopuszczony do pracy.",
            icon = Icons.Default.Build,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 3,
            question = "Ładunek na widłach ogranicza Ci widoczność do przodu. Co robisz?",
            answers = listOf(
                "Jadę przodem bardzo powoli",
                "Jadę tyłem, żeby mieć pełną widoczność",
                "Proszę współpracownika, żeby szedł kilka metrów przede mną na sygnalizację"
            ),
            correctAnswerIndex = 1,
            explanation = "Jeżeli ładunek ogranicza widoczność, operator powinien jechać tyłem. Prowadzenie wózka 'na komendy' osoby trzeciej jest niewystarczające.",
            icon = Icons.Default.Visibility,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 4,
            question = "Podczas skręcania na skrzyżowaniu alejek magazynowych prawidłowe zachowanie to:",
            answers = listOf(
                "Zwolnić, zatrąbić i zatrzymać się, jeśli nie ma pełnej widoczności",
                "Przyspieszyć, żeby szybciej je opuścić",
                "Nie zwalniać, jeśli wózek ma światła ostrzegawcze"
            ),
            correctAnswerIndex = 0,
            explanation = "Na skrzyżowaniach alejek należy bezwzględnie zwolnić i w razie ograniczonej widoczności zatrzymać się całkowicie przed wjazdem.",
            icon = Icons.Default.VolumeUp,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 5,
            question = "Podczas jazdy z ładunkiem na widłach prawidłowa wysokość wideł to:",
            answers = listOf(
                "Jak najwyżej, aby uniknąć uderzenia w nierówności podłogi",
                "Kilka centymetrów nad podłożem, stabilnie i nisko",
                "Na wysokości klatki piersiowej operatora"
            ),
            correctAnswerIndex = 1,
            explanation = "Ładunek powinien być transportowany możliwie nisko nad podłożem. Zmniejsza to ryzyko utraty stabilności.",
            icon = Icons.Default.KeyboardArrowDown,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 6,
            question = "Podczas ładowania akumulatorów wózków elektrycznych należy:",
            answers = listOf(
                "Zapewnić wentylację i zakaz używania otwartego ognia",
                "Ładować w dowolnym miejscu, jeśli nikomu nie przeszkadza",
                "Skrócić czas ładowania maksymalnym prądem"
            ),
            correctAnswerIndex = 0,
            explanation = "Podczas ładowania akumulatora wydziela się wodór – gaz wybuchowy. Wymagane jest właściwe miejsce ładowania i brak źródeł iskrzenia.",
            icon = Icons.Default.PropaneTank,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 7,
            question = "Na widłach wózka znajduje się osoba, która chce 'na szybko' podnieść się do regału. Co robisz?",
            answers = listOf(
                "Podnosisz ją ostrożnie, jeśli się trzyma",
                "Odmówisz i zgłosisz zdarzenie jako niezgodne z zasadami bezpieczeństwa",
                "Podnosisz ją tylko na niewielką wysokość"
            ),
            correctAnswerIndex = 1,
            explanation = "Podnoszenie ludzi na widłach lub palecie jest kategorycznie zabronione. To ciężkie naruszenie zasad BHP.",
            icon = Icons.Default.ReportProblem,
            category = SafetyCategory.FORKLIFT
        ),

        SafetyQuiz(
            id = 8,
            question = "Podczas cofania wózkiem bez asysty i bez pełnej widoczności należy:",
            answers = listOf(
                "Cofać bardzo szybko, żeby skrócić czas manewru",
                "Zatrzymać się i upewnić, że strefa za wózkiem jest bezpieczna",
                "Polegać wyłącznie na sygnale dźwiękowym cofania"
            ),
            correctAnswerIndex = 1,
            explanation = "Operator odpowiada za bezpieczeństwo manewru cofania. Jeśli nie ma pewności co do strefy za pojazdem, musi się zatrzymać.",
            icon = Icons.Default.Sync,
            category = SafetyCategory.FORKLIFT
        ),


        // --- PRACA NA WYSOKOŚCI / SKŁADOWANIE WYSOKIE ---

        SafetyQuiz(
            id = 9,
            question = "Paleta na regale wystaje poza obrys trawersów. Jak należy postąpić?",
            answers = listOf(
                "Zostawić, skoro się trzyma",
                "Natychmiast zgłosić i poprawić ułożenie palety",
                "Dofoliować paletę, żeby 'się nie zsunęła'"
            ),
            correctAnswerIndex = 1,
            explanation = "Paleta wystająca poza regał zagraża stabilności i może spaść. Wymaga korekty ułożenia.",
            icon = Icons.Default.Inventory,
            category = SafetyCategory.HEIGHT
        ),

        SafetyQuiz(
            id = 10,
            question = "Zauważasz brak lub nieczytelną tabliczkę dopuszczalnego obciążenia regału. Co robisz?",
            answers = listOf(
                "Składasz towar tak jak zwykle",
                "Opierasz się na obciążeniu z sąsiedniej półki",
                "Wstrzymujesz składowanie i zgłaszasz do przełożonego / BHP"
            ),
            correctAnswerIndex = 2,
            explanation = "Bez informacji o nośności nie wolno obciążać regału. Przeciążenie może doprowadzić do zawalenia.",
            icon = Icons.Default.Report,
            category = SafetyCategory.HEIGHT
        ),

        SafetyQuiz(
            id = 11,
            question = "W obszarze wysokiego składowania obowiązkowe środki ochrony indywidualnej to zazwyczaj:",
            answers = listOf(
                "Kamizelka ostrzegawcza i rękawice",
                "Kask ochronny i obuwie z noskiem ochronnym",
                "Okulary przeciwsłoneczne"
            ),
            correctAnswerIndex = 1,
            explanation = "Ryzyko spadających przedmiotów wymaga ochrony głowy i stóp. Środki OI są obowiązkowe, nie opcjonalne.",
            icon = Icons.Default.Construction,
            category = SafetyCategory.HEIGHT
        ),

        SafetyQuiz(
            id = 12,
            question = "Na słupku regału widzisz pęknięcie konstrukcyjne. Jak reagujesz?",
            answers = listOf(
                "Robisz zdjęcie i wracasz do pracy",
                "Natychmiast oznaczasz strefę jako niebezpieczną i zgłaszasz problem",
                "Ignorujesz, bo 'ktoś się tym zajmie'"
            ),
            correctAnswerIndex = 1,
            explanation = "Uszkodzenie konstrukcji regału stanowi zagrożenie dla życia. Strefę należy zabezpieczyć i wyłączyć z użytku.",
            icon = Icons.Default.Warning,
            category = SafetyCategory.HEIGHT
        ),

        SafetyQuiz(
            id = 13,
            question = "Czy wolno ręcznie podawać towar nad wysokość barków?",
            answers = listOf(
                "Tak, jeśli ładunek jest lekki",
                "Nie, należy korzystać z urządzeń pomocniczych",
                "Tak, ale tylko gdy druga osoba pomaga"
            ),
            correctAnswerIndex = 1,
            explanation = "Przenoszenie ponad wysokość barków zwiększa ryzyko urazu barku i upadku ładunku. Używaj wózka, podestu, podnośnika.",
            icon = Icons.Default.Height,
            category = SafetyCategory.HEIGHT
        ),

        SafetyQuiz(
            id = 14,
            question = "Czy wolno wspinać się na regał, aby zdjąć towar 'na szybko'?",
            answers = listOf(
                "Tak, jeśli robisz to ostrożnie",
                "Tak, jeśli druga osoba obserwuje",
                "Nie, należy użyć przeznaczonego sprzętu dostępowego (podest, drabina atestowana)"
            ),
            correctAnswerIndex = 2,
            explanation = "Wspinanie się na regał jest niedopuszczalne. Do pracy na wysokości stosuje się wyłącznie sprzęt przeznaczony do tego celu.",
            icon = Icons.Default.Block,
            category = SafetyCategory.HEIGHT
        ),


        // --- ERGONOMIA / PRACA RĘCZNA ---

        SafetyQuiz(
            id = 15,
            question = "Jaki jest prawidłowy sposób podnoszenia ciężkiego kartonu z podłogi?",
            answers = listOf(
                "Pochylić się w pasie przy prostych nogach",
                "Ugiąć kolana, utrzymać proste plecy, trzymać ładunek blisko ciała",
                "Szarpnąć szybko, żeby było krócej"
            ),
            correctAnswerIndex = 1,
            explanation = "Podnoś z użyciem nóg, nie kręgosłupa. Plecy powinny pozostać możliwie proste.",
            icon = Icons.Default.FitnessCenter,
            category = SafetyCategory.ERGONOMICS
        ),

        SafetyQuiz(
            id = 16,
            question = "Podczas obracania się z ładunkiem należy:",
            answers = listOf(
                "Skręcić sam tułów przy nieruchomych stopach",
                "Obrócić całe ciało wraz ze stopami",
                "Wykonać szybki skręt w pasie, żeby nie tracić czasu"
            ),
            correctAnswerIndex = 1,
            explanation = "Skręcanie kręgosłupa pod obciążeniem to jedna z głównych przyczyn urazów odcinka lędźwiowego.",
            icon = Icons.Default.AccessibilityNew,
            category = SafetyCategory.ERGONOMICS
        ),

        SafetyQuiz(
            id = 17,
            question = "Długotrwała praca w pozycji stojącej wymaga:",
            answers = listOf(
                "Braku przerw, żeby szybciej skończyć",
                "Regularnej zmiany pozycji ciała i krótkich przerw od obciążenia",
                "Stania nieruchomo w jednej pozycji"
            ),
            correctAnswerIndex = 1,
            explanation = "Zmiana pozycji zmniejsza przeciążenie stawów skokowych, kolan i odcinka lędźwiowego.",
            icon = Icons.Default.DirectionsWalk,
            category = SafetyCategory.ERGONOMICS
        ),

        SafetyQuiz(
            id = 18,
            question = "Jeżeli odczuwasz ból kręgosłupa podczas pracy fizycznej:",
            answers = listOf(
                "Przerywasz pracę i zgłaszasz to przełożonemu",
                "Przyjmujesz tabletkę przeciwbólową i pracujesz dalej",
                "Ignorujesz objawy, dopóki możesz chodzić"
            ),
            correctAnswerIndex = 0,
            explanation = "Ból jest sygnałem przeciążenia lub urazu. Kontynuowanie pracy może pogorszyć stan i doprowadzić do trwałego uszkodzenia.",
            icon = Icons.Default.HealthAndSafety,
            category = SafetyCategory.ERGONOMICS
        ),

        SafetyQuiz(
            id = 19,
            question = "Przy pracy w wysokiej temperaturze w magazynie należy:",
            answers = listOf(
                "Unikać picia wody, żeby nie robić przerw",
                "Pić regularnie małe ilości wody i monitorować samopoczucie",
                "Pracować szybciej, żeby wcześniej skończyć zmianę"
            ),
            correctAnswerIndex = 1,
            explanation = "Odwodnienie obniża koncentrację i zwiększa ryzyko zasłabnięcia oraz błędów operacyjnych.",
            icon = Icons.Default.WaterDrop,
            category = SafetyCategory.ERGONOMICS
        ),


        // --- EWAKUACJA / POŻAR / ZDARZENIE NIEBEZPIECZNE ---

        SafetyQuiz(
            id = 20,
            question = "Po ogłoszeniu alarmu pożarowego należy:",
            answers = listOf(
                "Udać się najkrótszą drogą ewakuacyjną do punktu zbiórki",
                "Skończyć aktualne zadanie i dopiero potem wyjść",
                "Sprawdzić, czy alarm jest 'prawdziwy'"
            ),
            correctAnswerIndex = 0,
            explanation = "Ewakuacja musi rozpocząć się natychmiast. Nie wolno kontynuować pracy ani wracać po rzeczy osobiste.",
            icon = Icons.Default.LocalFireDepartment,
            category = SafetyCategory.EVACUATION
        ),

        SafetyQuiz(
            id = 21,
            question = "Droga ewakuacyjna jest zastawiona paletami. Jak postępujesz?",
            answers = listOf(
                "Zgłaszasz natychmiast jako zagrożenie życia",
                "Przenosisz palety samodzielnie w trakcie pracy",
                "Ignorujesz, bo jest druga droga wyjścia"
            ),
            correctAnswerIndex = 0,
            explanation = "Zablokowana droga ewakuacyjna jest krytycznym naruszeniem zasad bezpieczeństwa i musi być zgłoszona natychmiast.",
            icon = Icons.Default.Block,
            category = SafetyCategory.EVACUATION
        ),

        SafetyQuiz(
            id = 22,
            question = "Czy wolno wracać do strefy ewakuowanej po pozostawione przedmioty osobiste?",
            answers = listOf(
                "Tak, jeśli 'szybko'",
                "Tak, jeśli masz zgodę przełożonego",
                "Nie, dopóki służby nie wydadzą zgody na powrót"
            ),
            correctAnswerIndex = 2,
            explanation = "Po ewakuacji powrót do strefy zagrożenia jest zabroniony do czasu formalnego odwołania alarmu.",
            icon = Icons.Default.ExitToApp,
            category = SafetyCategory.EVACUATION
        ),

        SafetyQuiz(
            id = 23,
            question = "Zauważasz zadymienie i zapach spalenizny, ale nie widzisz otwartego ognia. Co robisz najpierw?",
            answers = listOf(
                "Samodzielnie sprawdzasz źródło",
                "Nie reagujesz, bo nie ma płomienia",
                "Uruchamiasz alarm pożarowy i powiadamiasz przełożonego"
            ),
            correctAnswerIndex = 2,
            explanation = "Każdy podejrzany zapach spalenizny należy traktować jak potencjalny początek pożaru i natychmiast zgłosić.",
            icon = Icons.Default.Warning,
            category = SafetyCategory.EVACUATION
        ),

        SafetyQuiz(
            id = 24,
            question = "Czy możesz samodzielnie użyć gaśnicy?",
            answers = listOf(
                "Tak, jeśli pożar jest niewielki i nie narażasz swojego zdrowia",
                "Nie, tylko straż pożarna może używać gaśnic",
                "Tak, zawsze, niezależnie od sytuacji"
            ),
            correctAnswerIndex = 0,
            explanation = "Dopuszczalne jest gaszenie małych źródeł ognia przy zachowaniu własnego bezpieczeństwa i po wcześniejszym uruchomieniu alarmu.",
            icon = Icons.Default.FireExtinguisher,
            category = SafetyCategory.EVACUATION
        ),


        // --- PIERWSZA POMOC ---

        SafetyQuiz(
            id = 25,
            question = "Osoba leży na podłodze, jest nieprzytomna i nie oddycha. Pierwsze działanie:",
            answers = listOf(
                "Podnieść ją do pozycji siedzącej",
                "Wezwać pomoc (112) i rozpocząć resuscytację krążeniowo-oddechową",
                "Dać wodę do picia"
            ),
            correctAnswerIndex = 1,
            explanation = "Brak oddechu oznacza stan bezpośredniego zagrożenia życia. Konieczne jest natychmiastowe wezwanie pomocy i rozpoczęcie RKO.",
            icon = Icons.Default.Favorite,
            category = SafetyCategory.FIRST_AID
        ),

        SafetyQuiz(
            id = 26,
            question = "Jaki jest prawidłowy schemat RKO u osoby dorosłej?",
            answers = listOf(
                "30 uciśnięć klatki piersiowej i 2 oddechy ratownicze",
                "15 uciśnięć i 2 oddechy",
                "Tylko oddechy ratownicze"
            ),
            correctAnswerIndex = 0,
            explanation = "Standardowo stosuje się cykl 30:2 i tempo ucisków około 100–120/min, z uciśnięciem klatki na głębokość ok. 5–6 cm.",
            icon = Icons.Default.MonitorHeart,
            category = SafetyCategory.FIRST_AID
        ),

        SafetyQuiz(
            id = 27,
            question = "Współpracownik skaleczył się i krwawi intensywnie. Co robisz?",
            answers = listOf(
                "Zakładasz bezpośredni, silny ucisk czystym opatrunkiem",
                "Polewasz ranę wodą utlenioną przez kilka minut",
                "Zakręcasz rękę opaską uciskową niezależnie od rodzaju rany"
            ),
            correctAnswerIndex = 0,
            explanation = "Najważniejszy jest natychmiastowy, stały ucisk miejsca krwawienia. Opaska uciskowa jest stosowana tylko w szczególnych przypadkach.",
            icon = Icons.Default.Bloodtype,
            category = SafetyCategory.FIRST_AID
        ),

        SafetyQuiz(
            id = 28,
            question = "Do oka współpracownika dostała się substancja chemiczna. Co należy zrobić?",
            answers = listOf(
                "Płukać oko nieprzerwanie przez co najmniej 15 minut",
                "Przykryć oko opatrunkiem i czekać",
                "Polecić, żeby energicznie mrugał"
            ),
            correctAnswerIndex = 0,
            explanation = "Natychmiastowe, ciągłe płukanie przez minimum 15 minut ogranicza uszkodzenia chemiczne gałki ocznej.",
            icon = Icons.Default.RemoveRedEye,
            category = SafetyCategory.FIRST_AID
        ),

        SafetyQuiz(
            id = 29,
            question = "Widzisz osobę, która dławi się, nie może mówić i nie może nabrać powietrza. Co robisz?",
            answers = listOf(
                "Podajesz wodę do picia",
                "Uderzasz dłonią między łopatki i wykonujesz uciśnięcia nadbrzusza (chwyt Heimlicha)",
                "Sadzasz ją i czekasz, aż samo przejdzie"
            ),
            correctAnswerIndex = 1,
            explanation = "Zadławienie z całkowitą niedrożnością dróg oddechowych wymaga natychmiastowych działań ratunkowych.",
            icon = Icons.Default.PanTool,
            category = SafetyCategory.FIRST_AID
        ),

        SafetyQuiz(
            id = 30,
            question = "Współpracownik skarży się na zawroty głowy, mdłości i bardzo gorącą skórę po pracy w wysokiej temperaturze. Co to może oznaczać?",
            answers = listOf(
                "Udar cieplny – stan zagrożenia życia, konieczne chłodzenie i wezwanie pomocy",
                "Zwykłe zmęczenie, wystarczy odpocząć",
                "Prawdopodobnie zatrucie pokarmowe"
            ),
            correctAnswerIndex = 0,
            explanation = "Objawy udaru cieplnego wymagają natychmiastowego działania: przeniesienia w chłodne miejsce, chłodzenia oraz wezwania pomocy.",
            icon = Icons.Default.Thermostat,
            category = SafetyCategory.FIRST_AID
        )
    )

    fun getRandomQuiz(): SafetyQuiz = quizzes.random()
    fun getQuizzesByCategory(category: SafetyCategory): List<SafetyQuiz> =
        quizzes.filter { it.category == category }
    fun getAllQuizzes(): List<SafetyQuiz> = quizzes
}
