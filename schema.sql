-- Suppression des DROP TABLE pour préserver les données entre les lancements

CREATE OR REPLACE FUNCTION create_enum_role() RETURNS void AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enum_role') THEN
        CREATE TYPE enum_role AS ENUM ('Admin', 'Membre', 'Lecteur');
    END IF;
END;
$$ LANGUAGE plpgsql;
SELECT create_enum_role();

CREATE OR REPLACE FUNCTION create_enum_action() RETURNS void AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enum_action') THEN
        CREATE TYPE enum_action AS ENUM ('Création', 'Modification', 'Suppression');
    END IF;
END;
$$ LANGUAGE plpgsql;
SELECT create_enum_action();

CREATE TABLE IF NOT EXISTS Utilisateur (
    id_user VARCHAR PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    mail VARCHAR(100) UNIQUE NOT NULL,
    date_creation DATE DEFAULT CURRENT_DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS Groupe (
    id_groupe VARCHAR PRIMARY KEY,
    nom_groupe VARCHAR(100) NOT NULL,
    description TEXT,
    date_creation DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS Application (
    id_app VARCHAR PRIMARY KEY,
    nom_app VARCHAR(100) NOT NULL,
    chemin_exec VARCHAR(255) NOT NULL,
    version_app VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS Tag (
    id_tag VARCHAR PRIMARY KEY,
    nom_tag VARCHAR(50) NOT NULL,
    icone_tag VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Element (
    id_element VARCHAR PRIMARY KEY,
    nom_element VARCHAR(255) NOT NULL,
    date_creation DATE DEFAULT CURRENT_DATE NOT NULL,
    type_element VARCHAR(50) NOT NULL,
    date_modification DATE DEFAULT CURRENT_DATE,
    date_acces DATE DEFAULT CURRENT_DATE NOT NULL,
    est_cache BOOLEAN DEFAULT FALSE NOT NULL,
    emplacement VARCHAR NOT NULL,
    id_proprietaire VARCHAR REFERENCES Utilisateur(id_user) NOT NULL,
    id_parent VARCHAR REFERENCES Element(id_element)
);

CREATE TABLE IF NOT EXISTS Fichier (
    id_element VARCHAR PRIMARY KEY REFERENCES Element(id_element) ON DELETE CASCADE,
    taille_octets INT NOT NULL,
    extension VARCHAR(10),
    open_with_app VARCHAR REFERENCES Application(id_app) NOT NULL,
    icone VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Dossier (
    id_element VARCHAR PRIMARY KEY REFERENCES Element(id_element) ON DELETE CASCADE,
    icone VARCHAR(50) NOT NULL,
    nb_elements INT DEFAULT 0,
    id_app VARCHAR REFERENCES Application(id_app)
);

CREATE TABLE IF NOT EXISTS Membre_Groupe (
    id_user VARCHAR REFERENCES Utilisateur(id_user),
    id_groupe VARCHAR REFERENCES Groupe(id_groupe),
    role_user enum_role NOT NULL,
    date_rejoint DATE NOT NULL,
    PRIMARY KEY (id_user, id_groupe)
);

CREATE TABLE IF NOT EXISTS Element_Tag (
    id_element VARCHAR REFERENCES Element(id_element) ON DELETE CASCADE, -- AJOUT DU CASCADE
    id_tag VARCHAR REFERENCES Tag(id_tag) ON DELETE CASCADE, -- AJOUT DU CASCADE
    PRIMARY KEY (id_element, id_tag)
);

CREATE TABLE IF NOT EXISTS Historique (
    id_historique VARCHAR PRIMARY KEY,
    type_action enum_action NOT NULL,
    date_action DATE DEFAULT CURRENT_DATE,
    id_element VARCHAR NOT NULL, -- SUPPRESSION DU 'REFERENCES Element' (L'historique est désormais indépendant)
    id_user VARCHAR REFERENCES Utilisateur(id_user) NOT NULL
);

-- Utilisation de ON CONFLICT DO NOTHING pour ne pas écraser les données existantes
INSERT INTO Utilisateur (id_user, nom, prenom, mail, date_creation) VALUES 
('U01', 'Massé', 'Jean', 'jean@gmail.com', '2020-01-12'),
('U02', 'Dupont', 'Alice', 'alice@cyu.fr', '2024-09-01')
ON CONFLICT (id_user) DO NOTHING;

INSERT INTO Groupe (id_groupe, nom_groupe, description, date_creation) VALUES 
('G01', 'Projet SAÉ', 'Groupe de travail pour la base de données', '2025-03-26'),
('G02', 'Design Team', 'Ressources graphiques', '2025-04-01')
ON CONFLICT (id_groupe) DO NOTHING;

INSERT INTO Membre_Groupe (id_user, id_groupe, role_user, date_rejoint) VALUES 
('U01', 'G01', 'Admin', '2025-03-26'),
('U02', 'G01', 'Membre', '2025-03-27')
ON CONFLICT (id_user, id_groupe) DO NOTHING;

INSERT INTO Application (id_app, nom_app, chemin_exec, version_app) VALUES 
('APP_VLC', 'VLC Player', 'C:\Program Files\VLC\vlc.exe', 'v3.0.20'),
('APP_VISIO', 'Visionneuse', 'C:\Program Files\Images\vis.exe', 'v1.2'),
('APP_VSCODE', 'VS Code', 'C:\Program Files\Microsoft VS Code\code.exe', 'v1.88')
ON CONFLICT (id_app) DO NOTHING;

INSERT INTO Tag (id_tag, nom_tag, icone_tag) VALUES 
('T01', 'Urgent', 'warning.png'),
('T02', 'Brouillon', 'draft.png')
ON CONFLICT (id_tag) DO NOTHING;

INSERT INTO Element (id_element, nom_element, type_element, emplacement, id_proprietaire, id_parent) VALUES 
('E_DOSSIER_APPS', 'Application', 'Dossier', '/application', 'U01', NULL),
('E_TEXTE', 'Texte', 'Dossier', '/actions', 'U01', NULL),
('E_FICHIER', 'Fichier', 'Dossier', '/fichier', 'U01', NULL),
('E_TAG', 'TAG', 'Dossier', '/tag', 'U01', NULL),
('E02', 'Favoris', 'Dossier', '<HOME>/Favoris', 'U01', 'E_FICHIER'),
('E_VSCODE_LINK', 'Visual Studio Code', 'Dossier', '/apps/vscode', 'U01', 'E_DOSSIER_APPS'),
('E_VLC_LINK', 'VLC Player', 'Dossier', '/apps/vlc', 'U01', 'E_DOSSIER_APPS')
ON CONFLICT (id_element) DO NOTHING;

INSERT INTO Dossier (id_element, icone, id_app, nb_elements) VALUES 
('E_FICHIER', 'folder_home.png', NULL, 2),
('E_DOSSIER_APPS', 'folder_apps.png', NULL, NULL),
('E_VSCODE_LINK', 'icone_vscode.png', 'APP_VSCODE', NULL)
ON CONFLICT (id_element) DO NOTHING;

INSERT INTO Element_Tag (id_element, id_tag) VALUES 
('E02', 'T01')
ON CONFLICT (id_element, id_tag) DO NOTHING;

INSERT INTO Historique (id_historique, type_action, date_action, id_element, id_user) VALUES 
('H01', 'Création', '2026-05-10', 'E02', 'U01')
ON CONFLICT (id_historique) DO NOTHING;
