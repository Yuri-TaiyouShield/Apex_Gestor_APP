import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

@Injectable()
export class ApexRouteReuseStrategy implements RouteReuseStrategy {
  shouldDetach(_route: ActivatedRouteSnapshot): boolean {
    void _route;
    return false;
  }

  store(_route: ActivatedRouteSnapshot, _handle: DetachedRouteHandle | null): void {
    void _route;
    void _handle;
    // No-op: operational screens must load their current route context every time.
  }

  shouldAttach(_route: ActivatedRouteSnapshot): boolean {
    void _route;
    return false;
  }

  retrieve(_route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    void _route;
    return null;
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, current: ActivatedRouteSnapshot): boolean {
    if (future.routeConfig !== current.routeConfig) {
      return false;
    }
    return future.data?.['entity'] === current.data?.['entity']
      && future.data?.['mode'] === current.data?.['mode']
      && future.params === current.params
      && future.queryParams === current.queryParams;
  }
}
