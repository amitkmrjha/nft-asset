CREATE TABLE nft_asset.nft_assets(
    asset_id VARCHAR(36) not NULL,
    name VARCHAR(400),
    description TEXT,
    fileId VARCHAR(36),
    status VARCHAR(50) ,
    status_message TEXT ,
    PRIMARY KEY(asset_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.nft_assets TO "nft_asset";
GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.nft_assets TO "nft_asset";
