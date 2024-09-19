CREATE INDEX CONCURRENTLY state_tag_idx on nft_asset.durable_state (tag);
CREATE INDEX CONCURRENTLY state_global_offset_idx on nft_asset.durable_state (global_offset);
