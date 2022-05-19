import { Component, OnInit, ElementRef, Renderer2 } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CheckoutDeliveryFacade, CheckoutPaymentFacade, CheckoutFacade } from '@spartacus/checkout/root';
import { ViewChild, EventEmitter } from '@angular/core'
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CheckoutStepService, PaymentFormComponent} from '@spartacus/checkout/components';
import { UserPaymentService, GlobalMessageService, TranslationService, ActiveCartService, PaymentDetails, Address, AddressValidation, Country, StateUtils, Region, UserAddressService } from '@spartacus/core';
import { CheckoutService, CheckoutDeliveryService, CheckoutPaymentService } from '@spartacus/checkout/core';
import * as i0 from "@angular/core";
import { ScriptService } from "./services/script.service";
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { map, filter, tap, shareReplay, switchMap, skipWhile, debounceTime, take, withLatestFrom, distinctUntilChanged, takeWhile } from 'rxjs/operators';
import * as i4 from "@spartacus/storefront";
import { Card, ICON_TYPE, ModalRef, ModalService, SuggestedAddressDialogComponent } from '@spartacus/storefront';
import { I18nModule, provideDefaultConfig, B2BUserRole, GlobalMessageType, OCC_USER_ID_ANONYMOUS, OCC_USER_ID_GUEST, EMAIL_PATTERN, UrlModule, ConfigModule, isNotUndefined, B2BPaymentTypeEnum, DaysOfWeek, recurrencePeriod, ORDER_TYPE, provideConfig, PromotionLocation, FeaturesConfigModule } from '@spartacus/core';

const SCRIPT_PATH = 'https://cdn.novalnet.de/js/v2/NovalnetUtility.js';
declare let NovalnetUtility: any;
declare var data: any;
declare var address: any;
declare var result: any;
declare var country: any;


@Component({
  selector: 'app-add-custom-payments',
  templateUrl: './add-custom-payments.component.html',
  styleUrls: ['./add-custom-payments.component.scss']
})
export class AddCustomPaymentsComponent implements OnInit {
    static ɵcmp: i0.ɵɵComponentDeclaration<PaymentFormComponent, "cx-payment-form", never, { "setAsDefaultField": "setAsDefaultField"; "paymentMethodsCount": "paymentMethodsCount"; }, { "goBack": "goBack"; "closeForm": "closeForm"; "setPaymentDetails": "setPaymentDetails"; }, never, never>;
    iconTypes: typeof ICON_TYPE;
    suggestedAddressModalRef: ModalRef | null;
    months: string[];
    years: number[];
    
    shipaddress: any;
    shipping_address: any;
    address_data: any;
    shippingAddress$: Observable<Address>;
    countries$: Observable<Country[]>;
    loading$: Observable<StateUtils.LoaderState<void>>;
    sameAsShippingAddress: boolean;
    iframeloaded: boolean;
    regions$: Observable<Region[]>;
    selectedCountry$: BehaviorSubject<string>;
    showSameAsShippingAddressCheckbox$: Observable<boolean>;
    setAsDefaultField: boolean;
    goBack: EventEmitter<any>;
    closeForm: EventEmitter<any>;
    setPaymentDetails: EventEmitter<any>;
    paymentForm: FormGroup;
    billingAddressForm: FormGroup;
    paymentMethodsCount: number;
    constructor(private elementRef: ElementRef,
    private renderer: Renderer2,
    private scriptService: ScriptService,
    private checkoutPaymentService: CheckoutPaymentFacade,
    private checkoutDeliveryService: CheckoutDeliveryFacade,
    private userPaymentService: UserPaymentService,
    private globalMessageService: GlobalMessageService,
    private fb: FormBuilder,
    private modalService: ModalService,
    private userAddressService: UserAddressService,
    private activatedRoute: ActivatedRoute,
    private checkoutStepService: CheckoutStepService,
    ) { 
        this.suggestedAddressModalRef = null;
        this.checkoutPaymentService = checkoutPaymentService;
        this.checkoutDeliveryService = checkoutDeliveryService;
        this.userPaymentService = userPaymentService;
        this.globalMessageService = globalMessageService;
        this.fb = fb;
        this.paymentMethodsCount = 0;
        this.modalService = modalService;
        this.userAddressService = userAddressService;
        this.iconTypes = ICON_TYPE;
        this.months = [];
        this.shipping_address = {};
        this.address_data = {};
        this.years = [];
        this.sameAsShippingAddress = true;
        this.iframeloaded = false;
        this.setAsDefaultField = true;
        this.selectedCountry$ = new BehaviorSubject('');
        this.activatedRoute = activatedRoute;
        this.goBack = new EventEmitter();
        //~ this.goBack = "goBack";
        this.closeForm = new EventEmitter();
        this.setPaymentDetails = new EventEmitter();
      
        this.countries$ = this.userPaymentService.getAllBillingCountries().pipe(tap((countries) => {
            // If the store is empty fetch countries. This is also used when changing language.
            if (Object.keys(countries).length === 0) {
                this.userPaymentService.loadBillingCountries();
            }
        }));
        
        this.billingAddressForm = this.fb.group({
            firstName: ['', Validators.required],
            lastName: ['', Validators.required],
            line1: ['', Validators.required],
            line2: [''],
            town: ['', Validators.required],
            region: this.fb.group({
                isocodeShort: [null, Validators.required],
            }),
            country: this.fb.group({
                isocode: [null, Validators.required],
            }),
            postalCode: ['', Validators.required],
        });
        
        this.paymentForm = this.fb.group({
            paymentType: [''],
            pan_hash: [''],
            unique_id: [''],
            do_redirect: ['']
        });
        
        //~ this.paymentForm = this.fb.group({});
        
        this.shippingAddress$ = this.checkoutDeliveryService.getDeliveryAddress();
        this.loading$ =
            this.checkoutPaymentService.getSetPaymentDetailsResultProcess();
        this.showSameAsShippingAddressCheckbox$ = combineLatest([
            this.countries$,
            this.shippingAddress$,
        ]).pipe(map(([countries, address]) => {
            var _a;
            this.shipaddress = address;
            return ((_a = ((address === null || address === void 0 ? void 0 : address.country) &&
                !!countries.filter((country) => { var _a; return country.isocode === ((_a = address.country) === null || _a === void 0 ? void 0 : _a.isocode); }).length)) !== null && _a !== void 0 ? _a : false);
        }), tap((shouldShowCheckbox) => {
            this.sameAsShippingAddress = shouldShowCheckbox;
        }));
        this.regions$ = this.selectedCountry$.pipe(switchMap((country) => this.userAddressService.getRegions(country)), tap((regions) => {
            const regionControl = this.billingAddressForm.get('region.isocodeShort');
            if (regions.length > 0) {
                regionControl === null || regionControl === void 0 ? void 0 : regionControl.enable();
            }
            else {
                regionControl === null || regionControl === void 0 ? void 0 : regionControl.disable();
            }
        }));
    }

    ngOnInit(): void {
		
	}
    
    public loadiframe() {
		var self = this.paymentForm;
		const scriptElement = this.scriptService.loadJsScript(this.renderer, SCRIPT_PATH);
		scriptElement.onload = () => {
			console.log(this.shipaddress.firstName);
			NovalnetUtility.setClientKey("0f84e6cf6fe1b93f1db8198aa2eae719");
			var request_object = {
				callback: {
					on_success: function(data : any) {
						(<HTMLInputElement>document.getElementById('pan_hash')).value = data['hash'];
						(<HTMLInputElement>document.getElementById('unique_id')).value = data['unique_id'];
						(<HTMLInputElement>document.getElementById('do_redirect')).value = data['do_redirect'];
						//~ (<HTMLInputElement>document.getElementsByName('checkout_submit')).click();
						let element: HTMLElement = document.getElementsByClassName('checkout_submit')[0] as HTMLElement;
						element.click();
					},
					on_error: function on_error(data : any) {
						alert(data['error_message']);
					},
					on_show_captcha: function on_show_captcha() {
						//~ window.scrollTo(0, 200);
					}
				},
				iframe: {
			 
					id: "novalnet_iframe",
					
					inline: 1,
					
					style: {
						container: "",
						
						input: "",
						
						label: ""
					},
					
					text: {
					
						lang : "EN",
						
						error: "Your credit card details are invalid",
						
						card_holder : {
						
							label: "Card holder name",
							
							place_holder: "Name on card",
							
							error: "Please enter the valid card holder name"
						},
						card_number : {
						
							label: "Card number",
							
							place_holder: "XXXX XXXX XXXX XXXX",
							
							error: "Please enter the valid card number"
						},
						expiry_date : {
							label: "Expiry date",
							error: "Please enter the valid expiry month / year in the given format"
						},
						cvc : {
							label: "CVC/CVV/CID",
							place_holder: "XXX",
							error: "Please enter the valid CVC/CVV/CID"
						}
					}
				},
			
				customer: {
					first_name: "Max",
					last_name: "Mustermann",
					email: "test@novalnet.de",
				},
				
				transaction: {
					amount: 100,
					currency: "EUR",
					test_mode: 1,
					skip_auth: 1
				},
				custom: {
					lang: "EN"
				}
			};
			if(!this.iframeloaded) {
				NovalnetUtility.createCreditCardForm(request_object);
				this.iframeloaded = true;
			}
		}
    }
    
    public toggleSameAsShippingAddress() {
        this.sameAsShippingAddress = !this.sameAsShippingAddress;
        //~ this.iframeloaded = false;
		//~ this.loadiframe();
    }
    public getAddressCardContent(address: any) {
        var _a;
        let region = '';
        if (address.region && address.region.isocode) {
            region = address.region.isocode + ', ';
        }
        return {
            textBold: address.firstName + ' ' + address.lastName,
            text: [
                address.line1,
                address.line2,
                address.town + ', ' + region + ((_a = address.country) === null || _a === void 0 ? void 0 : _a.isocode),
                address.postalCode,
                address.phone,
            ],
        };
    }
    public openSuggestedAddress(results:any) {
        if (!this.suggestedAddressModalRef) {
            this.suggestedAddressModalRef = this.modalService.open(SuggestedAddressDialogComponent, { centered: true, size: 'lg' });
            this.suggestedAddressModalRef.componentInstance.enteredAddress =
                this.billingAddressForm.value;
            this.suggestedAddressModalRef.componentInstance.suggestedAddresses =
                results.suggestedAddresses;
            this.suggestedAddressModalRef.result
                .then(() => {
                this.suggestedAddressModalRef = null;
            })
                .catch(() => {
                // this  callback is called when modal is closed with Esc key or clicking backdrop
                this.suggestedAddressModalRef = null;
            });
        }
    }
    
    public close() {
        this.closeForm.emit();
    }
    
    public back() {
        
    }
    
    public verifyAddress() {
        if (this.sameAsShippingAddress) {
            this.next();
        }
        else {
            this.userAddressService
                .verifyAddress(this.billingAddressForm.value)
                .subscribe((result) => {
                this.handleAddressVerificationResults(result);
            });
        }
    }
    
    public handleAddressVerificationResults(results:any) {
        if (results.decision === 'ACCEPT') {
            this.next();
        }
        else if (results.decision === 'REJECT') {
            this.globalMessageService.add({ key: 'addressForm.invalidAddress' }, GlobalMessageType.MSG_TYPE_ERROR);
        }
        else if (results.decision === 'REVIEW') {
            this.openSuggestedAddress(results);
        }
    }
    
    public countrySelected(country:any) {
        var _a;
        (_a = this.billingAddressForm.get('country.isocode')) === null || _a === void 0 ? void 0 : _a.setValue(country.isocode);
        this.selectedCountry$.next(country.isocode);
    }
    
    public next() {
		console.log((<HTMLInputElement>document.getElementById('paymentType')).value);
		console.log((<HTMLInputElement>document.getElementById('pan_hash')).value);
		if((<HTMLInputElement>document.getElementById('paymentType')).value == 'CreditCard' && (<HTMLInputElement>document.getElementById('pan_hash')).value == "") {
			console.log("hashcall");
			NovalnetUtility.getPanHash();
		} else {
			console.log("insubmit");
			this.checkoutStepService.next(this.activatedRoute);
		}
    }

  
}

