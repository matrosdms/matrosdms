-- =====================================================================
-- VW_CONTEXT
-- Optimized: Calculates item count ('sum') via subquery join.
-- Filters out archived contexts and items.
-- =====================================================================
CREATE OR REPLACE VIEW VW_CONTEXT AS
SELECT 
    c.CONTEXT_ID,
    c.uuid,
    c.name,
    c.description,
    c.icon,
    c.stage,
    c.date_created,
    c.date_updated,
    c.date_archived,
    c.date_run_until,
    c.version,
    -- COALESCE ensures we get '0' instead of 'null' for empty folders
    CAST(COALESCE(stats.item_count, 0) AS INTEGER) as sum
FROM 
    context c
LEFT JOIN (
    -- Pre-calculate counts. This is the speed optimization.
    SELECT 
        context_id, 
        COUNT(*) as item_count 
    FROM 
        item 
    WHERE 
        date_archived IS NULL 
    GROUP BY 
        context_id
) stats ON stats.context_id = c.context_id
WHERE 
    c.date_archived IS NULL
ORDER BY 
    c.name;
    
-- =====================================================================
-- VW_SEARCH
-- Optimized: Uses standard joins. Calculates 'ELEMENT_ARCHIVED' dynamically.
-- =====================================================================
CREATE OR REPLACE VIEW VW_SEARCH AS
SELECT DISTINCT
    c.context_id            AS CONTEXT_ID,
    c.name                  AS CON_NAME,
    c.uuid                  AS CON_UUID,
    c.stage                 AS CON_STAGE,
    i.item_id               AS ITEM_ID,
    i.name                  AS ITEM_NAME,
    i.uuid                  AS ITEM_UUID,
    i.issue_date            AS ITEM_ISSUEDATE,
    c.date_archived         AS CON_DATEARCHIVED,
    i.date_archived         AS ITEM_DATEARCHIVED,
    CASE 
        WHEN i.date_archived IS NOT NULL OR c.date_archived IS NOT NULL 
        THEN TRUE 
        ELSE FALSE 
    END                     AS ELEMENT_ARCHIVED,
    i.store_id              AS STORE_STORE_ID,
    i.storage_item_identifier AS STORAGEITEMIDENTIFIER
FROM context c
JOIN item i ON i.context_id = c.context_id;


-- =====================================================================
-- VW_MASTERDATA_UUID
-- Optimized: Unions definition tables. Uses 'dbuser' (new table name).
-- =====================================================================
CREATE OR REPLACE VIEW VW_MASTERDATA_UUID AS 
SELECT x.TAB_TYPE, x.ID, x.UUID FROM (
    SELECT 'ATTRIBUTETYPE' AS TAB_TYPE, attributetype_id AS ID, uuid FROM attributetype 
    UNION ALL
    SELECT 'CATEGORY'      AS TAB_TYPE, category_id      AS ID, uuid FROM category
    UNION ALL
    SELECT 'STORE'         AS TAB_TYPE, store_id         AS ID, uuid FROM store
    UNION ALL
    SELECT 'USER'          AS TAB_TYPE, user_id          AS ID, uuid FROM dbuser 
) x
ORDER BY 1, 2;


-- =====================================================================
-- VW_TRANSACTIONDATA_UUID
-- Optimized: Unions transactional data. Replaces legacy 'EVENT' with 'ACTION'.
-- =====================================================================
CREATE OR REPLACE VIEW VW_TRANSACTIONDATA_UUID AS 
SELECT x.TAB_TYPE, x.ID, x.UUID FROM (
    SELECT 'CONTEXT' AS TAB_TYPE, context_id AS ID, uuid FROM context
    UNION ALL 
    SELECT 'ACTION'  AS TAB_TYPE, action_id  AS ID, uuid FROM action
    UNION ALL
    SELECT 'ITEM'    AS TAB_TYPE, item_id    AS ID, uuid FROM item  
) x 
ORDER BY 1, 2;


-- =====================================================================
-- VW_SEARCH_ATTRIBUTES
-- Optimized: No longer joins a separate 'attribute' table (N+1 killer).
-- Instead, it inspects the 'attributes' JSON column on the Item table directly.
-- FAST: This calculation is instant compared to the old GROUP BY JOIN.
-- =====================================================================
CREATE OR REPLACE VIEW VW_SEARCH_ATTRIBUTES AS 
SELECT DISTINCT 
    v.CONTEXT_ID, 
    v.CON_NAME, 
    v.CON_UUID, 
    v.CON_STAGE, 
    v.ITEM_ID, 
    v.ITEM_NAME, 
    v.ITEM_UUID, 
    v.ITEM_ISSUEDATE, 
    v.CON_DATEARCHIVED, 
    v.ITEM_DATEARCHIVED, 
    v.ELEMENT_ARCHIVED, 
    v.STORE_STORE_ID, 
    v.STORAGEITEMIDENTIFIER,
    -- New Logic: Check if JSON column has content (> 2 chars means not just '{}')
    CASE 
        WHEN i.attributes IS NOT NULL AND LENGTH(CAST(i.attributes AS VARCHAR)) > 2 THEN 1 
        ELSE 0 
    END AS ATT_COUNT
FROM VW_SEARCH v 
JOIN item i ON v.ITEM_ID = i.item_id
ORDER BY v.CONTEXT_ID, v.ITEM_ISSUEDATE;