import { ActivatedRoute } from '@angular/router';
import { OnInit } from '@angular/core';
import { CheckoutStepService, PaymentMethodComponent as CorePaymentMethodComponent } from '@spartacus/checkout/components';
import { UserPaymentService, GlobalMessageService, TranslationService, ActiveCartService, PaymentDetails } from '@spartacus/core';
import { CheckoutService, CheckoutDeliveryService, CheckoutPaymentService } from '@spartacus/checkout/core';
import * as i0 from "@angular/core";
export declare class NovalnetPaymentMethodComponent extends CorePaymentMethodComponent implements OnInit {
    protected userPaymentService: UserPaymentService;
    protected checkoutService: CheckoutService;
    protected checkoutDeliveryService: CheckoutDeliveryService;
    protected checkoutPaymentService: CheckoutPaymentService;
    protected globalMessageService: GlobalMessageService;
    protected activatedRoute: ActivatedRoute;
    protected translation: TranslationService;
    protected activeCartService: ActiveCartService;
    protected checkoutStepService: CheckoutStepService;
    showCallbackScreen: boolean;
    isDpCallback(): boolean;
    hideCallbackScreen(): void;
    paymentDetailsAdded(paymentDetails: PaymentDetails): void;
    constructor(userPaymentService: UserPaymentService, checkoutService: CheckoutService, checkoutDeliveryService: CheckoutDeliveryService, checkoutPaymentService: CheckoutPaymentService, globalMessageService: GlobalMessageService, activatedRoute: ActivatedRoute, translation: TranslationService, activeCartService: ActiveCartService, checkoutStepService: CheckoutStepService);
    static ɵfac: i0.ɵɵFactoryDeclaration<NovalnetPaymentMethodComponent, never>;
    static ɵcmp: i0.ɵɵComponentDeclaration<NovalnetPaymentMethodComponent, "cx-payment-method", never, {}, {}, never, never>;
}
