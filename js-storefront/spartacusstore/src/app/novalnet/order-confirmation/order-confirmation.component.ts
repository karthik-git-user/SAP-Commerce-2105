import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, Sanitizer } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { EventService, GlobalMessageService, GlobalMessageType, OccEndpointsService, TranslationService } from '@spartacus/core';
import { CheckoutQueryResetEvent } from '@spartacus/checkout/base/root';
import { OrderDetailsService } from '@spartacus/order/components';
import { ReturnRequestService } from '@spartacus/order/components/return-request-detail/return-request.service';
import { OrderService } from '@spartacus/order/core';
import { Order, OrderFacade, OrderHistoryFacade, OrderPlacedEvent } from '@spartacus/order/root';
import { Observable } from 'rxjs';
import { filter, shareReplay, take, tap, withLatestFrom } from 'rxjs/operators';
import { NovalnetService } from '../novalnet-service/novalnet-service';
import { ClearCartDialogComponentService } from '@spartacus/cart/base/components';
import { ActiveCartService, CartActions } from '@spartacus/cart/base/core';

@Component({
  selector: 'app-order-confirmation',
  templateUrl: './order-confirmation.component.html',
  styleUrls: ['./order-confirmation.component.scss']
})
export class OrderConfirmationComponent implements OnDestroy  {
  orderFacade: OrderFacade;
  globalMessageService: GlobalMessageService;
  translationService: TranslationService;
  isGuestCustomer: boolean;
  orderGuid: any;
  order$!: Observable<Order | undefined>;
  novalnetService: NovalnetService;
  placedOrder: any;
  orderService: OrderService;
  occEndpoint: OccEndpointsService;
  clearCartDialogComponentService : ClearCartDialogComponentService;
  http: HttpClient;
  paymentDetails: any;
  sanitizer: DomSanitizer;
  message: any;
  orderDetailsService: OrderDetailsService;
  orderHistoryFacade: OrderHistoryFacade;
  eventService : EventService;
  activeCartService : ActiveCartService;
  showComments: boolean;

  constructor(orderFacade:OrderFacade, globalMessageService:GlobalMessageService, translationService:TranslationService, novalnetService:NovalnetService, orderService:OrderService, occEndpoint:OccEndpointsService, http:HttpClient, sanitizer:DomSanitizer, orderDetailsService: OrderDetailsService, orderHistoryFacade: OrderHistoryFacade, eventService:EventService, clearCartDialogComponentService: ClearCartDialogComponentService, activeCartService:ActiveCartService) {
    this.orderFacade = orderFacade;
    this.globalMessageService = globalMessageService;
    this.translationService = translationService;
    this.orderDetailsService =orderDetailsService;
    this.isGuestCustomer = false;
    this.novalnetService = novalnetService;
    this.orderHistoryFacade =orderHistoryFacade;
    this.orderService = orderService;
    this.occEndpoint = occEndpoint;
    this.sanitizer = sanitizer;
    this.http = http;
    this.eventService = eventService;
    this.clearCartDialogComponentService = clearCartDialogComponentService;
    this.activeCartService = activeCartService;
    this.placedOrder = this.novalnetService.getOrder();
   
    this.orderService.setPlacedOrder(this.placedOrder);
     this.showComments = false;
    
    
  }

  ngOnInit() {
	  
  }
  
  ngAfterViewInit() {
	  let requestURL = this.occEndpoint.getBaseUrl()+ "/novalnet/orders/paymentDetails?orderno="+this.placedOrder.code;
	  this.http.post(requestURL, {"orderno" : "this.placedOrder.code"}).subscribe((data:any) => {console.log(data); this.message =  data.comments;this.showComments = true;
		  document.getElementsByClassName('nn_txn_message')[0].innerHTML += data.comments;});
		  // new CartActions.ClearCartState();
		  //~ this.clearCartDialogComponentService.deleteActiveCart();
		  // this.activeCartService.reloadActiveCart();
      this.eventService.dispatch({this.placedOrder}, OrderPlacedEvent);
  }
  
  

  ngOnDestroy() {
      this.orderFacade.clearPlacedOrder();
  }

}
