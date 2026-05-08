package com.hyp.enums;

public enum DeliveryPartner {
    PIDGE,
    ADLOGGS,
    MANUAL,

    /** @deprecated Replaced by DeliveryModel.SELF — kept for existing data compatibility */
    @Deprecated
    SELF,

    /** @deprecated Replaced by DeliveryModel.DEDICATED — kept for existing data compatibility */
    @Deprecated
    DEDICATED_RIDER,

    /** @deprecated No longer used — kept for existing data compatibility */
    @Deprecated
    PLATFORM
}
