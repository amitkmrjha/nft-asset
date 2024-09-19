GRANT SELECT, INSERT ON nft_asset.event_journal TO "nft_asset";
GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.event_tag TO "nft_asset";
GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.snapshot TO "nft_asset";
GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.durable_state TO "nft_asset";

GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.akka_projection_offset_store TO "nft_asset";
GRANT SELECT, INSERT, UPDATE, DELETE ON nft_asset.akka_projection_management TO "nft_asset";

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA nft_asset TO "nft_asset";

--
