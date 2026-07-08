# GitHub Setup — Build APKs via GitHub Actions

## 1. Créer le repo GitHub

```bash
# Initialiser Git
cd MaidCleaner
git init
git add .
git commit -m "Initial commit: MaidCleaner Android app"

# Créer le repo sur GitHub (depuis le navigateur ou via gh CLI)
gh repo create MaidCleaner --public --source=. --push
# OU manuellement:
git remote add origin https://github.com/TON_USERNAME/MaidCleaner.git
git branch -M main
git push -u origin main
```

## 2. Le build se lance automatiquement

Dès que tu pousses sur `main`, le workflow `.github/workflows/build.yml` tourne.

### Récupérer l'APK :
1. Va sur ton repo GitHub
2. Onglet **Actions**
3. Clique sur le dernier workflow run
4. En bas : **Artifacts** → `maidcleaner-debug` → télécharge le ZIP contenant l'APK

## 3. Build manuel (sans push)

Va sur **Actions** → **Build APK** → **Run workflow** → bouton vert.

## 4. Release signée (APK signé pour distribution)

### Configurer les secrets GitHub

1. Génère un keystore :
```bash
keytool -genkey -v -keystore maidcleaner.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias maidcleaner
```

2. Encode en base64 :
```bash
# Linux/Mac:
base64 -w 0 maidcleaner.jks > maidcleaner.b64

# Windows PowerShell:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("maidcleaner.jks")) | Out-File maidcleaner.b64
```

3. Ajoute les secrets dans **Settings → Secrets and variables → Actions** :

| Secret Name | Valeur |
|---|---|
| `KEYSTORE_BASE64` | Contenu de `maidcleaner.b64` |
| `KEYSTORE_PASSWORD` | Ton mot de passe keystore |
| `KEY_ALIAS` | `maidcleaner` |
| `KEY_PASSWORD` | Ton mot de passe de clé |

### Créer une release signée

```bash
git tag v1.0.0
git push origin v1.0.0
```

Le workflow `release.yml` se déclenche automatiquement, build l'APK signé,
et crée une GitHub Release avec l'APK en pièce jointe.

## 5. Workflows disponibles

| Fichier | Déclencheur | Résultat |
|---|---|---|
| `build.yml` | Push sur main/PR/Manual | APK debug + release unsigned + tests |
| `release.yml` | Tag `v*` / Manual | APK signé + GitHub Release |
