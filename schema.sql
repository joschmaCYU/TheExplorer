DROP TABLE IF EXISTS Corbeille, Historique, Element_Tag, Membre_Groupe, Dossier, Fichier, Element, Tag, Application, Groupe, Utilisateur CASCADE;
DROP TYPE IF EXISTS enum_role, enum_action CASCADE;

CREATE TYPE enum_role AS ENUM ('Admin', 'Membre', 'Lecteur');
CREATE TYPE enum_action AS ENUM ('Création', 'Modification', 'Suppression');

CREATE TABLE Utilisateur (
    id_user VARCHAR PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    mail VARCHAR(100) UNIQUE NOT NULL,
    date_creation DATE DEFAULT CURRENT_DATE NOT NULL
);

CREATE TABLE Groupe (
    id_groupe VARCHAR PRIMARY KEY,
    nom_groupe VARCHAR(100) NOT NULL,
    description TEXT,
    date_creation DATE NOT NULL
);

CREATE TABLE Application (
    id_app VARCHAR PRIMARY KEY,
    nom_app VARCHAR(100) NOT NULL,
    chemin_exec VARCHAR(255) NOT NULL,
    version_app VARCHAR(20) NOT NULL
);

CREATE TABLE Tag (
    id_tag VARCHAR PRIMARY KEY,
    nom_tag VARCHAR(50) NOT NULL,
    icone_tag VARCHAR(50) NOT NULL
);

CREATE TABLE Element (
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

CREATE TABLE Fichier (
    id_element VARCHAR PRIMARY KEY REFERENCES Element(id_element) ON DELETE CASCADE,
    taille_octets INT NOT NULL,
    extension VARCHAR(10),
    open_with_app VARCHAR REFERENCES Application(id_app) NOT NULL,
    icone VARCHAR(50) NOT NULL
);

CREATE TABLE Dossier (
    id_element VARCHAR PRIMARY KEY REFERENCES Element(id_element) ON DELETE CASCADE,
    icone VARCHAR(50) NOT NULL,
    nb_elements INT DEFAULT 0
);

-- Tables d'associations N-N
CREATE TABLE Membre_Groupe (
    id_user VARCHAR REFERENCES Utilisateur(id_user),
    id_groupe VARCHAR REFERENCES Groupe(id_groupe),
    role_user enum_role NOT NULL,
    date_rejoint DATE NOT NULL,
    PRIMARY KEY (id_user, id_groupe)
);

CREATE TABLE Element_Tag (
    id_element VARCHAR REFERENCES Element(id_element),
    id_tag VARCHAR REFERENCES Tag(id_tag),
    PRIMARY KEY (id_element, id_tag)
);

CREATE TABLE Historique (
    id_historique VARCHAR PRIMARY KEY,
    type_action enum_action NOT NULL,
    date_action DATE DEFAULT CURRENT_DATE,
    id_element VARCHAR REFERENCES Element(id_element) NOT NULL,
    id_user VARCHAR REFERENCES Utilisateur(id_user) NOT NULL
);

CREATE TABLE Corbeille (
    id_corbeille VARCHAR PRIMARY KEY,
    date_ajout DATE DEFAULT CURRENT_DATE NOT NULL,
    date_suppression_def DATE,
    id_element VARCHAR REFERENCES Element(id_element) UNIQUE NOT NULL
);

INSERT INTO Utilisateur (id_user, nom, prenom, mail, date_creation) VALUES 
('U01', 'Massé', 'Jean', 'jean@gmail.com', '2020-01-12'),
('U02', 'Dupont', 'Alice', 'alice@cyu.fr', '2024-09-01');

INSERT INTO Groupe (id_groupe, nom_groupe, description, date_creation) VALUES 
('G01', 'Projet SAÉ', 'Groupe de travail pour la base de données', '2025-03-26'),
('G02', 'Design Team', 'Ressources graphiques', '2025-04-01');

INSERT INTO Membre_Groupe (id_user, id_groupe, role_user, date_rejoint) VALUES 
('U01', 'G01', 'Admin', '2025-03-26'),
('U02', 'G01', 'Membre', '2025-03-27');

INSERT INTO Application (id_app, nom_app, chemin_exec, version_app) VALUES 
('APP1', 'Visionneuse', 'C:\Program Files\Images\vis.exe', 'v1.2'),
('APP2', 'VLC Player', 'C:\Program Files\VLC\vlc.exe', 'v3.0.20'),
('APP3', 'VS Code', 'C:\Program Files\Microsoft VS Code\code.exe', 'v1.88');

INSERT INTO Tag (id_tag, nom_tag, icone_tag) VALUES 
('T01', 'Urgent', 'warning.png'),
('T02', 'Brouillon', 'draft.png');

INSERT INTO Element (id_element, nom_element, type_element, emplacement, id_proprietaire, id_parent) VALUES 
('E01', 'Racine_Jean', 'Dossier', '/home/jean', 'U01', NULL),
('E02', 'Vidéos_SAE', 'Dossier', '/home/jean/videos', 'U01', 'E01'),
('E03', 'logo.png', 'Fichier', '/home/jean', 'U01', 'E01'),
('E04', 'demo.mp4', 'Fichier', '/home/jean/videos', 'U01', 'E02');

INSERT INTO Dossier (id_element, icone, nb_elements) VALUES 
('E01', 'folder_home.png', 2),
('E02', 'folder_video.png', 1);

INSERT INTO Fichier (id_element, taille_octets, extension, open_with_app, icone) VALUES 
('E03', 102450, 'png', 'APP1', 'file_image.png'),
('E04', 15480000, 'mp4', 'APP2', 'file_video.png');

INSERT INTO Element_Tag (id_element, id_tag) VALUES 
('E04', 'T01');

INSERT INTO Historique (id_historique, type_action, date_action, id_element, id_user) VALUES 
('H01', 'Création', '2026-05-10', 'E03', 'U01'),
('H02', 'Création', '2026-05-11', 'E04', 'U01');

--bonjuir le man