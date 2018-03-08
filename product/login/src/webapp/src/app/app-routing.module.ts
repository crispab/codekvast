import {ForbiddenComponent} from './pages/forbidden.component';
import {HomeComponent} from './pages/home.component';
import {IsAuthenticated} from './guards/isAuthenticated';
import {LoginComponent} from './pages/login.component';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {StartComponent} from './pages/start.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
    }, {
        path: 'forbidden',
        component: ForbiddenComponent
    }, {
        path: 'home',
        component: HomeComponent
    }, {
        path: 'login',
        component: LoginComponent
    }, {
        path: 'start',
        component: StartComponent,
        canActivate: [IsAuthenticated]
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
