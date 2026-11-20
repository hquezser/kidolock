# Accessibilité

Mode Enfant est pensé pour des enfants en bas âge : l'interface évite la lecture et les
petits gestes imprécis partout où c'est possible.

## Ce qui est déjà prévu

- **Guidage vocal** : les avertissements de fin de session sont annoncés à voix haute en
  FR, EN ou ES, y compris en mode silencieux (volume média, annonces pré-enregistrées).
- **Grands éléments tactiles** : applis en grand, pavé du code parent dimensionné pour
  des doigts d'enfant, boutons « +5 min » / « +15 min » larges.
- **Compréhension sans lecture** : le temps restant est aussi matérialisé par un disque
  qui change de couleur (vert → rouge) et un compte à rebours visuel ; les textes restent
  disponibles en trois langues.
- **Pas de dépendance à la langue du système** : la langue est choisie par le parent dans
  les réglages.

## Environnements supportés

Android 8 et plus (minSdk 26). Validé sur Nexus 10 / LineageOS 17.1 (Android 10). Le mode
paysage est pris en charge sur l'écran parent.

## Limitations connues

- L'espace enfant n'a pas été testé avec TalkBack : les écrans sont volontairement simples
  (grille d'icônes, gros boutons) mais ne sont pas audités pour la navigation exhaustive
  par lecteur d'écran.
- Le disque de temps repose aussi sur la couleur : un daltonisme sévère peut rendre la
  nuance vert/rouge moins parlante (le compte à rebours chiffré reste disponible).
- Les annonces vocales couvrent les avertissements planifiés ; les écrans de fin et
  d'erreur ne sont pas tous doublés vocalement.

## Signaler une barrière

Si un élément vous empêche d'utiliser l'application (enfant, parent ou aidant), ouvrez
une issue avec le template « accessibilité » ou contactez le mainteneur. Précisez
l'appareil, la version d'Android et ce qui bloque.
