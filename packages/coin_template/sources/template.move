module 0x0::template;

use sui::coin;

/// The OTW for the Coin
public struct TEMPLATE has drop {}

const DECIMALS: u8 = 6;
const SYMBOL: vector<u8> = b"TMPL";
const NAME: vector<u8> = b"Template Coin";
const DESCRIPTION: vector<u8> = b"Template Coin Description";

/// Init the Coin
fun init(witness: TEMPLATE, ctx: &mut TxContext) {
    let (treasury, metadata) = coin::create_currency(
        witness, DECIMALS, SYMBOL, NAME, DESCRIPTION, option::none(), ctx
    );

    transfer::public_transfer(treasury, tx_context::sender(ctx));
    transfer::public_transfer(metadata, tx_context::sender(ctx));
}