# Politique de sécurité

## Versions supportées

Seule la dernière version publiée est supportée. Mode Enfant n'a pas de cycle de
publication régulier : la branche `main` reflète l'état courant.

## Signaler une vulnérabilité

**N'ouvrez pas d'issue publique** pour un problème de sécurité.

Utilisez le [rapport de vulnérabilité privé de GitHub](../../security/advisories/new)
(Security → Report a vulnerability) ou contactez directement le mainteneur via son profil
GitHub. Décrivez le problème, les étapes pour le reproduire et l'impact estimé.

Vous recevrez une réponse sous 7 jours. Si le problème est confirmé, un correctif est
préparé et coordonné avec vous avant publication.

## Périmètre

L'application demande le service d'accessibilité Android et voit les noms des applis au
premier plan pendant une session. Un problème de confidentialité, de contournement du
verrouillage, ou une fuite de données est dans le périmètre. Les limites volontaires
documentées dans le README (contournable en mode sans échec ou via adb) ne sont pas des
vulnérabilités.
