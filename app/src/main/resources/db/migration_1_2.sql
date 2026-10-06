CREATE TABLE IF NOT EXISTS `node_sessions` (`id` TEXT NOT NULL, `endpoint` TEXT NOT NULL, `certificateSha256` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `encryptedSeed` TEXT NOT NULL, `scopes` TEXT NOT NULL, `state` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `operations` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `target` TEXT NOT NULL, `state` TEXT NOT NULL, `payload` TEXT NOT NULL, `result` TEXT, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `contributions` (`id` TEXT NOT NULL, `nodeFingerprint` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `drafts` (`id` TEXT NOT NULL, `payload` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`));
