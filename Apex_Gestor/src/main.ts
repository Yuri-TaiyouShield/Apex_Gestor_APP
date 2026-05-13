import { bootstrapApplication } from '@angular/platform-browser';
import { addIcons } from 'ionicons';
import {
  add,
  bagHandleOutline,
  barChartOutline,
  businessOutline,
  cardOutline,
  cartOutline,
  cashOutline,
  cubeOutline,
  desktopOutline,
  documentTextOutline,
  fileTrayFullOutline,
  homeOutline,
  logInOutline,
  menuOutline,
  peopleOutline,
  personCircleOutline,
  phonePortraitOutline,
  pricetagsOutline,
  receiptOutline,
  searchOutline,
  shieldCheckmarkOutline,
  settingsOutline,
  storefrontOutline,
  syncOutline,
  trailSignOutline,
  trashOutline,
  trendingUpOutline
} from 'ionicons/icons';

import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

addIcons({
  add,
  bagHandleOutline,
  barChartOutline,
  businessOutline,
  cardOutline,
  cartOutline,
  cashOutline,
  cubeOutline,
  desktopOutline,
  documentTextOutline,
  fileTrayFullOutline,
  homeOutline,
  logInOutline,
  menuOutline,
  peopleOutline,
  personCircleOutline,
  phonePortraitOutline,
  pricetagsOutline,
  receiptOutline,
  searchOutline,
  shieldCheckmarkOutline,
  settingsOutline,
  storefrontOutline,
  syncOutline,
  trailSignOutline,
  trashOutline,
  trendingUpOutline
});

bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
