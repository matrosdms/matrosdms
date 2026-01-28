-- Reflects the Enum ERootCategory

INSERT INTO Category (CATEGORY_ID, uuid, name, description, ordinal, object)
VALUES (-1, 'ROOT_WHO', 'WHO', 'Persons / Groups', 0, false);

-- 2. WHAT
INSERT INTO Category (CATEGORY_ID, uuid, name, description, ordinal, object)
VALUES (-2, 'ROOT_WHAT', 'WHAT', 'Topics / Content', 0, false);

-- 3. WHERE (The UUID string 'ROOT_WHERE' is safe)
INSERT INTO Category (CATEGORY_ID, uuid, name, description, ordinal, object)
VALUES (-3, 'ROOT_WHERE', 'WHERE', 'Locations', 0, false);

-- 4. KIND
INSERT INTO Category (CATEGORY_ID, uuid, name, description, ordinal, object)
VALUES (-4, 'ROOT_KIND', 'KIND', 'Document Type', 0, false);
