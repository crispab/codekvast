import {AuthTokenRenewer} from './guards/auth-token-renewer';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {HomeComponent} from './pages/home/home.component';
import {IsLoggedIn} from './guards/is-logged-in';
import {LoggedOutComponent} from './pages/logged-out/logged-out.component';
import {MethodDetailComponent} from './pages/methods/method-detail.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgModule} from '@angular/core';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {RouterModule, Routes} from '@angular/router';
import {SsoComponent} from './components/sso.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
    }, {
        path: 'home',
        component: HomeComponent,
        canActivate: [AuthTokenRenewer]
    }, {
        path: 'logged-out/:source/:sourceApp',
        component: LoggedOutComponent
    }, {
        path: 'methods',
        component: MethodsComponent,
        canActivate: [IsLoggedIn, AuthTokenRenewer]
    }, {
        path: 'method/:id',
        component: MethodDetailComponent,
        canActivate: [IsLoggedIn, AuthTokenRenewer]
    }, {
        path: 'sso/:token/:navData',
        component: SsoComponent
    }, {
        path: 'status',
        component: CollectionStatusComponent,
        canActivate: [IsLoggedIn, AuthTokenRenewer]
    }, {
        path: 'reports',
        component: ReportGeneratorComponent,
        canActivate: [IsLoggedIn, AuthTokenRenewer]
    }, {
        path: 'vote-result/:feature/:vote',
        component: VoteResultComponent,
        canActivate: [IsLoggedIn, AuthTokenRenewer]
    }, {
        path: '**',
        redirectTo: 'home',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
