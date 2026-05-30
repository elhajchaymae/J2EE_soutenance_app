# ENSAH - Système de Gestion des Soutenances

Application Spring Boot complète pour la planification automatique des soutenances de stage à l'ENSAH (École Nationale des Sciences Appliquées d'Al-Hoceima).

## 🚀 Démarrage rapide

### Prérequis
- Java 17+
- Maven 3.6+

### Lancer l'application
```bash
cd soutenance-app
mvn spring-boot:run
```
Puis ouvrir : http://localhost:8080

---

## 📋 Fonctionnalités

### 1. Import Excel (multi-feuilles)
- Détection automatique des feuilles par mots-clés
- Import : filières, professeurs, salles, étudiants, jours de soutenance
- Rapport détaillé des erreurs et avertissements

### 2. Matching automatique Étudiant ↔ Encadrant
- Score = 60% compatibilité spécialité + 40% équité de charge
- Distribution équitable garantie
- Réinitialisation possible

### 3. Génération du Planning
**Contraintes respectées :**
- Pas de deux soutenances consécutives pour un même prof (pause d'1h minimum)
- Jury de 3 membres : Encadrant + Président + Examinateur
- Pas de conflit de salle ni de prof
- Distribution équitable des jurys
- Créneaux : 8h-18h, 1h par soutenance

### 4. Exports
- **Planning** : Excel (une feuille par jour + récap) et PDF (paysage)
- **Matching** : Excel et PDF groupés par filière

### 5. Aperçu web
- Dashboard avec tableau de bord
- Vue planning par jour avec cartes jury
- Tables étudiants, professeurs, salles

---

## 🗂️ Structure du projet

```
src/main/java/ma/ensah/soutenance/
├── entity/          # Entités JPA (Filiere, Professeur, Etudiant, Soutenance, MembreJury, Salle, JourSoutenance)
├── repository/      # Repositories Spring Data JPA
├── service/         # Logique métier
│   ├── ExcelImportService.java    # Import Excel auto-détection
│   ├── MatchingService.java       # Algorithme matching
│   ├── PlanningService.java       # Algorithme planning + contraintes
│   ├── ExcelExportService.java    # Export Excel
│   └── PdfExportService.java      # Export PDF (iText)
├── controller/
│   ├── MainController.java        # Routes web Thymeleaf
│   └── ExportController.java      # Routes téléchargement
└── SoutenanceApplication.java
```

---

## ⚙️ Configuration (`application.properties`)

```properties
# Durée d'une soutenance (minutes)
app.soutenance.duree-minutes=60

# Créneaux horaires
app.soutenance.heure-debut=8
app.soutenance.heure-fin=18

# Base de données : H2 par défaut (fichier local)
# Pour MySQL, remplacer par :
# spring.datasource.url=jdbc:mysql://localhost:3306/soutenances
# spring.datasource.username=root
# spring.datasource.password=...
# spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## 📊 Format Excel d'entrée

Voir `EXEMPLE_EXCEL.md` pour le format détaillé.

**Les en-têtes sont détectés automatiquement** par mots-clés. Noms de feuilles reconnus :
| Données | Mots-clés acceptés dans le nom de feuille |
|---------|-------------------------------------------|
| Filières | filiere, filière, formation |
| Professeurs | prof, enseignant, encadrant, jury |
| Salles | salle, local, amphi |
| Étudiants | etudiant, étudiant, stagiaire, stage |
| Jours | jour, date, planning, calendrier |

---

## 🔧 Évolutions prévues / TODOs

- [ ] NLP pour amélioration du matching (analyse sémantique des sujets)
- [ ] Interface d'édition manuelle du planning (drag & drop)
- [ ] Authentification (Spring Security)
- [ ] Notifications email (JavaMailSender)
- [ ] Export PDF amélioré avec logo ENSAH
- [ ] API REST pour intégration future
- [ ] Migration vers MySQL/PostgreSQL en production

---

## 🗄️ Console H2 (développement)
http://localhost:8080/h2-console  
JDBC URL : `jdbc:h2:file:./data/soutenances`

---

## 🏫 ENSAH - Al-Hoceima
École Nationale des Sciences Appliquées d'Al-Hoceima
