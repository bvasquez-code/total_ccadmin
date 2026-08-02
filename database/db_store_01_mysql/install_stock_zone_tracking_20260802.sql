SET @MenuCod = 'SE000002';

INSERT INTO `app_menu`
(`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `CreationDate`, `Status`)
VALUES
(@MenuCod, 'Stock por zona', 'Consulta del stock actual por zona lógica en product_info', 'N', 'SE000000', 'CENTRAL', NOW(), 'A')
ON DUPLICATE KEY UPDATE
`Name` = VALUES(`Name`),
`Description` = VALUES(`Description`),
`IsMenuDad` = VALUES(`IsMenuDad`),
`MenuDadCod` = VALUES(`MenuDadCod`),
`ModifyUser` = 'CENTRAL',
`ModifyDate` = NOW(),
`Status` = 'A';

INSERT INTO `profile_menu`
(`ProfileCod`, `MenuCod`, `CreationUser`, `CreationDate`, `Status`)
SELECT eligible_profile.ProfileCod, @MenuCod, 'CENTRAL', NOW(), 'A'
FROM (
    SELECT DISTINCT profile.ProfileCod
    FROM app_profile profile
    WHERE profile.Status = 'A'
      AND (
            profile.ProfileCod = 'root'
            OR EXISTS (
                SELECT 1
                FROM profile_menu current_permission
                WHERE current_permission.ProfileCod = profile.ProfileCod
                  AND current_permission.MenuCod IN ('SE000000', 'PR000004')
                  AND current_permission.Status = 'A'
            )
      )
) eligible_profile
WHERE NOT EXISTS (
    SELECT 1
    FROM profile_menu existing_permission
    WHERE existing_permission.ProfileCod = eligible_profile.ProfileCod
      AND existing_permission.MenuCod = @MenuCod
);

UPDATE `profile_menu`
SET `ModifyUser` = 'CENTRAL',
    `ModifyDate` = NOW(),
    `Status` = 'A'
WHERE `MenuCod` = @MenuCod
  AND `ProfileCod` IN (
      SELECT ProfileCod
      FROM (
          SELECT DISTINCT profile.ProfileCod
          FROM app_profile profile
          WHERE profile.Status = 'A'
            AND (
                  profile.ProfileCod = 'root'
                  OR EXISTS (
                      SELECT 1
                      FROM profile_menu current_permission
                      WHERE current_permission.ProfileCod = profile.ProfileCod
                        AND current_permission.MenuCod IN ('SE000000', 'PR000004')
                        AND current_permission.Status = 'A'
                  )
            )
      ) eligible_profile
  );
