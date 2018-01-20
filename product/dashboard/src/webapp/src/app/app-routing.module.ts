import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {HomeComponent} from './pages/home/home.component';
import {IsLoggedIn} from './guards/is-logged-in';
import {LoggedOutComponent} from './pages/auth/logged-out.component';
import {MethodDetailComponent} from './pages/methods/method-detail.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgModule} from '@angular/core';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {RouterModule, Routes} from '@angular/router';
import {SsoComponent} from './components/sso.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';
import {NotLoggedInComponent} from './pages/auth/not-logged-in.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
    }, {
        path: 'home',
        component: HomeComponent
    }, {
        path: 'logged-out',
        component: LoggedOutComponent
    }, {
        path: 'methods',
        component: MethodsComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'method/:id',
        component: MethodDetailComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'not-logged-in',
        component: NotLoggedInComponent
    }, {
        path: 'sso/:token/:navData',
        component: SsoComponent
    }, {
        path: 'status',
        component: CollectionStatusComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'reports',
        component: ReportGeneratorComponent
    }, {
        path: 'vote-result/:feature/:vote',
        component: VoteResultComponent
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
