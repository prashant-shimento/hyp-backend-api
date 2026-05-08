package com.hyp.enums;

public enum DeliveryModel {
    SELF, // restaurant handles delivery — no entity, no tracking
    MANUAL, // ops books via Ola/Uber — entity created, tracked manually
    DEDICATED, // restaurant's own in-house rider — entity created, tracked manually
    PARTNER // external API partner (Pidge/Adloggs) — routed via DeliveryRouter
}
