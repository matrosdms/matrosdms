-- 1. Sender (Generic)
INSERT INTO Attributetype (
    attributetype_id, uuid, name, description, data_type, ordinal, date_created, version, icon, built_in
) VALUES (
    -102, 'ATTR_SENDER', 'Sender', 'Originator (Email or Person)', 0, 10, CURRENT_TIMESTAMP, 0, 'send', TRUE
);

-- 2. Recipient (Generic)
INSERT INTO Attributetype (
    attributetype_id, uuid, name, description, data_type, ordinal, date_created, version, icon, built_in
) VALUES (
    -103, 'ATTR_RECIPIENT', 'Recipient', 'Addressee', 0, 20, CURRENT_TIMESTAMP, 0, 'person_pin', TRUE
);

-- 3. Tax Year
INSERT INTO Attributetype (
    attributetype_id, uuid, name, description, data_type, ordinal, date_created, version, icon, built_in
) VALUES (
    -104, 'ATTR_TAXYEAR', 'Tax Year', 'Fiscal Year', 3, 30, CURRENT_TIMESTAMP, 0, 'event_note', TRUE
);

-- 4. Amount
INSERT INTO Attributetype (
    attributetype_id, uuid, name, description, data_type, ordinal, date_created, version, icon, built_in
) VALUES (
    -105, 'ATTR_AMOUNT', 'Amount', 'Total Value', 4, 40, CURRENT_TIMESTAMP, 0, 'euro_symbol', TRUE
);