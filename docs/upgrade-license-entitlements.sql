-- Apex Gestor - upgrade de licenciamento por app
-- Execute uma vez em bancos existentes antes de publicar a versao com protecao por pacote.

ALTER TABLE license_activation
  ADD COLUMN IF NOT EXISTS app_id varchar(40) NOT NULL DEFAULT 'web-client' AFTER app_version,
  ADD COLUMN IF NOT EXISTS license_plan varchar(40) NOT NULL DEFAULT 'LEGACY_ALL' AFTER app_id,
  ADD COLUMN IF NOT EXISTS licensed_apps varchar(160) NOT NULL DEFAULT 'desktop,mobile-staff,mobile-client,web-client' AFTER license_plan;

ALTER TABLE license_activation
  DROP INDEX unq_license_device;

ALTER TABLE license_activation
  ADD UNIQUE KEY unq_license_device_app (license_key_hash, device_hash, app_id),
  ADD KEY idx_license_app_status (license_key_hash, app_id, status);
