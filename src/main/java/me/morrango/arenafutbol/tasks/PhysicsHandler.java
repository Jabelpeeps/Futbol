package me.morrango.arenafutbol.tasks;

import me.morrango.arenafutbol.FutbolPlugin;

/**
 * ArenaFutbol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArenaFutbol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ArenaFutbol.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Morrango, Europia79
 */
public class PhysicsHandler implements Runnable {
    
    FutbolPlugin plugin;
    
    public PhysicsHandler(FutbolPlugin reference) {
        this.plugin = reference;
    }
    
    @Override
    public void run() {
        this.plugin.doBallPhysics();
    }
}
