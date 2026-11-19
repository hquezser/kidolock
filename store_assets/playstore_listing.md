# Fiche Google Play — KidoLock

> Limites officielles : titre 30 car., description courte 80 car., description complète 4000 car.
> Déployer `privacy.html` sur GitHub Pages et reporter l'URL dans la fiche + la déclaration.

## Titre (30 car. max)

| Langue | Texte | Longueur |
|---|---|---|
| FR | `KidoLock : Minuteur & Verrou` | 28 |
| EN | `KidoLock: Timer & App Lock` | 27 |
| ES | `KidoLock: Temporizador y bloqueo` | 34 ⚠️ |

**ES corrigé (≤30)** : `KidoLock: Control tiempo` (26) ou `KidoLock: Minutero y apps` (28)

## Description courte (80 car. max)

| Langue | Texte | Longueur |
|---|---|---|
| FR | `Prêtez la tablette sans stress : temps de jeu limité et applis autorisées.` | 78 |
| EN | `Lend the tablet stress-free: limited play time and approved apps only.` | 69 |
| ES | `Presta la tableta sin estrés: tiempo de juego limitado y apps autorizadas.` | 76 |

## Description complète (4000 car. max)

### FR (~1500 car.)

```
KidoLock transforme la tablette familiale en espace de jeu encadré : le parent choisit le temps de session et les applications autorisées, l'enfant profite d'un écran simplifié avec ses applis préférées.

COMMENT ÇA MARCHE
1. Le parent crée un code PIN (ou un calcul de secours s'il l'oublie).
2. Il choisit les applications autorisées et la durée : 15 à 90 minutes.
3. L'enfant voit un espace dédié : grandes tuiles colorées, sablier et robot compagnon.
4. Les applications non autorisées sont automatiquement recouvertes ; l'enfant est ramené à son espace.
5. Des avertissements progressifs (10, 5, 2, 1 minute) avec annonces vocales et mascotte rassurent l'enfant avant la fin.
6. À la fin, l'écran « recharge » explique gentiment que la batterie est vide — zéro crise.

PENSÉ POUR LES PARENTS
- Code PIN à 4 chiffres + question de calcul en secours.
- Ajout de 5 ou 15 minutes possible pendant la session (appui long sur le cadenas).
- Annonces vocales activables, robot « petit » ou « grand », interface en français, anglais ou espagnol.
- Fonctionne hors-ligne, sans compte, sans publicité, sans collecte de données.

POURQUOI LE SERVICE D'ACCESSIBILITÉ ?
KidoLock utilise le service d'accessibilité Android pour détecter l'application au premier plan pendant une session enfant et appliquer le verrouillage. Il est strictement inactif hors session. Aucune donnée n'est lue, conservée ni transmise.

KidoLock est un projet libre : pas de tracker, pas de SDK tiers, tout se passe sur l'appareil.
```

### EN (~1500 car.)

```
KidoLock turns the family tablet into a framed play space: the parent picks the session time and allowed apps, the child gets a simplified home screen with favorite apps.

HOW IT WORKS
1. The parent creates a 4-digit PIN (or a math challenge as a backup).
2. They choose the allowed apps and the duration: 15 to 90 minutes.
3. The child sees a dedicated space: big colorful tiles, an hourglass and a companion robot.
4. Unauthorized apps are automatically covered; the child is brought back to their space.
5. Progressive warnings (10, 5, 2, 1 minute left) with voice announcements and a mascot reassure the child before the end.
6. At the end, a friendly "battery empty" screen explains the session is over — no tantrums.

BUILT FOR PARENTS
- 4-digit PIN + math fallback question.
- Add 5 or 15 minutes during the session (long-press the lock).
- Optional voice announcements, young or older robot style, UI in French, English or Spanish.
- Works offline, no account, no ads, no data collection.

WHY THE ACCESSIBILITY SERVICE?
KidoLock uses the Android accessibility service to detect the foreground app during a child session and enforce the lock. It is strictly inactive outside sessions. No data is read, stored or transmitted.

KidoLock is open source: no trackers, no third-party SDKs, everything stays on the device.
```

### ES (~1500 car.)

```
KidoLock convierte la tableta familiar en un espacio de juego con límites: el padre elige el tiempo de sesión y las aplicaciones permitidas, el niño disfruta de una pantalla simplificada con sus apps favoritas.

CÓMO FUNCIONA
1. El padre crea un código PIN de 4 dígitos (o un cálculo de respaldo si lo olvida).
2. Elige las aplicaciones autorizadas y la duración: de 15 a 90 minutos.
3. El niño ve un espacio dedicado: grandes fichas de colores, un reloj de arena y un robot compañero.
4. Las aplicaciones no autorizadas se cubren automáticamente y el niño vuelve a su espacio.
5. Avisos progresivos (quedan 10, 5, 2, 1 minutos) con anuncios de voz y una mascota tranquilizan al niño antes del final.
6. Al terminar, una pantalla amigable de «batería descargada» explica que la sesión acabó, sin dramas.

PENSADO PARA PADRES
- PIN de 4 dígitos + pregunta de cálculo de respaldo.
- Añadir 5 o 15 minutos durante la sesión (pulsación larga en el candado).
- Anuncios de voz opcionales, robot «pequeño» o «mayor», interfaz en francés, inglés o español.
- Funciona sin conexión, sin cuenta, sin publicidad, sin recopilación de datos.

¿POR QUÉ EL SERVICIO DE ACCESIBILIDAD?
KidoLock usa el servicio de accesibilidad de Android para detectar la aplicación en primer plano durante una sesión infantil y aplicar el bloqueo. Permanece inactivo fuera de las sesiones. No se leen, almacenan ni transmiten datos.

KidoLock es de código abierto: sin rastreadores, sin SDK de terceros, todo ocurre en el dispositivo.
```

## Déclaration des autorisations — « Accessibility API » (Play Console)

**Utilisation de l'API Accessibility** :

> KidoLock is a parental-control app. During an active child session (started by the parent with a PIN), it uses the accessibility service to detect which app is in the foreground so it can cover unauthorized apps and return the child to the dedicated kids' home screen. The service also delivers timed voice/visual warnings as the session nears its end.
>
> The service is strictly passive outside an active session: it performs no reads, writes or monitoring when no session is running. No data is collected, stored or transmitted; all state is kept locally on the device. The app discloses this use in a prominent in-app dialog before redirecting the parent to the system accessibility settings.
>
> Video demonstration: [LIEN VERS store_assets/kidolock_compliance.mp4 HÉBERGÉ]

**Justification « usage des données »** : aucune — l'app ne déclare aucune collecte de données (Data safety : « No data collected/shared »).

## Data safety (Play Console)

- Collecte de données : **Aucune**
- Partage : **Aucun**
- Chiffrement en transit : N/A (pas de réseau)
- Suppression de compte : N/A (pas de compte)
