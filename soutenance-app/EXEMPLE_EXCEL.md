# Fichier Excel d'exemple - ENSAH Soutenances

## Format attendu : classeur Excel (.xlsx) avec plusieurs feuilles

### Feuille "filieres" (ou "filiere", "formation")
| code | nom                          | description             |
|------|------------------------------|-------------------------|
| GI   | Génie Informatique           | Formation en informatique |
| GC   | Génie Civil                  | Construction et BTP     |
| GE   | Génie Electrique             | Electricité et énergie  |
| GM   | Génie Mécanique              | Mécanique industrielle  |

### Feuille "professeurs" (ou "prof", "enseignants")
| nom      | prenom  | email                  | grade | specialite             |
|----------|---------|------------------------|-------|------------------------|
| Alami    | Ahmed   | a.alami@ensah.ma       | PH    | IA,NLP,BigData         |
| Benali   | Fatima  | f.benali@ensah.ma      | PA    | Réseaux,Sécurité       |
| Chakri   | Hassan  | h.chakri@ensah.ma      | MC    | Génie Logiciel,Web     |
| Darif    | Khadija | k.darif@ensah.ma       | PH    | IA,Machine Learning    |
| Essaidi  | Youssef | y.essaidi@ensah.ma     | PA    | Bases de données,Cloud |
| Fathi    | Nadia   | n.fathi@ensah.ma       | MC    | Mobile,Android         |

### Feuille "salles" (ou "salle", "locaux")
| nom | batiment | capacite | type  |
|-----|----------|----------|-------|
| S1  | Bloc A   | 30       | SALLE |
| S2  | Bloc A   | 30       | SALLE |
| S3  | Bloc B   | 40       | SALLE |
| A1  | Amphi    | 150      | AMPHI |

### Feuille "etudiants" (ou "etudiant", "stagiaires")
| cne    | nom      | prenom   | email                | sujet                                      | mots_cles         | entreprise    | filiere |
|--------|----------|----------|----------------------|--------------------------------------------|-------------------|---------------|---------|
| P12345 | Amrani   | Leila    | l.amrani@etu.ma      | Système de recommandation basé sur le NLP  | NLP,IA,Python     | TechMaroc     | GI      |
| P12346 | Berrada  | Omar     | o.berrada@etu.ma     | Application mobile de gestion de stock     | Mobile,Android    | LogisticPlus  | GI      |
| P12347 | Chraibi  | Imane    | i.chraibi@etu.ma     | Réseau de capteurs IoT pour smart building | Réseaux,IoT       | SmartCity     | GI      |
| P12348 | Diouri   | Karim    | k.diouri@etu.ma      | Plateforme e-learning avec ML              | ML,IA,Web         | EduTech       | GI      |
| P12349 | El Idrissi| Sara    | s.elidrissi@etu.ma   | Sécurité des applications web              | Sécurité,Web      | CyberMaroc    | GI      |

### Feuille "jours" (ou "jour", "dates", "calendrier")
| date       | description    |
|------------|----------------|
| 2025-06-10 | Jour 1 - Matin |
| 2025-06-11 | Jour 2         |
| 2025-06-12 | Jour 3 - Final |

---
Notes :
- Les en-têtes sont détectés automatiquement (insensibles à la casse)
- Les spécialités peuvent être séparées par virgule, point-virgule ou slash
- Le code filière dans la colonne "filiere" de la feuille étudiants doit correspondre au code de la feuille filières
- Les dates peuvent être au format Excel natif ou texte YYYY-MM-DD
