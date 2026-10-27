//
//  ConfettiView.swift
//  mars
//
//  Created by Mina on 22.05.2023.
//

import Foundation
import UIKit

extension UIViewController {
    
    func confettiBurst(duration:CGFloat = 3.0, birthRate:Float = 3, yAcceleration:CGFloat = 150)
    {
        let confettiTypes: [ConfettiType] = {
            let confettiColors = [
                (r:149,g:58,b:255), (r:255,g:195,b:41), (r:255,g:101,b:26),
                (r:123,g:92,b:255), (r:76,g:126,b:255), (r:71,g:192,b:255),
                (r:255,g:47,b:39), (r:255,g:91,b:134), (r:233,g:122,b:208)
            ].map { UIColor(red: $0.r / 255.0, green: $0.g / 255.0, blue: $0.b / 255.0, alpha: 1) }
            
            // For each position x shape x color, construct an image
            return [ConfettiPosition.foreground, ConfettiPosition.background].flatMap { position in
                return [ConfettiShape.rectangle, ConfettiShape.circle].flatMap { shape in
                    return confettiColors.map { color in
                        return ConfettiType(color: color, shape: shape, position: position)
                    }
                }
            }
        }()
        
        let confettiCells: [CAEmitterCell] = confettiTypes.map { confettiType in
                let cell = CAEmitterCell()
                cell.name = confettiType.name
                cell.beginTime = 0.1
                cell.birthRate = birthRate
                cell.contents = confettiType.image.cgImage
                cell.emissionRange = CGFloat(Double.pi)
                cell.lifetime = 10
                cell.scale = 0.75
                cell.spin = 4
                cell.spinRange = 8
                cell.velocityRange = 100
                cell.yAcceleration = yAcceleration
                cell.setValue("plane", forKey: "particleType")
                cell.setValue(Double.pi, forKey: "orientationRange")
                cell.setValue(Double.pi / 2, forKey: "orientationLongitude")
                cell.setValue(Double.pi / 2, forKey: "orientationLatitude")
                return cell
        }
        
        let confettiLayer: CAEmitterLayer = {
            let emitterLayer = CAEmitterLayer()
            
            emitterLayer.birthRate = 0
            emitterLayer.emitterCells = confettiCells
            emitterLayer.emitterPosition = CGPoint(x: view.bounds.midX, y: view.bounds.minY - 500)
            emitterLayer.emitterSize = CGSize(width: view.bounds.width, height: view.bounds.height)
            emitterLayer.emitterShape = .sphere
            emitterLayer.frame = view.bounds
            
            emitterLayer.beginTime = CACurrentMediaTime()
            
            return emitterLayer
        }()
        
        func addBirthrateAnimation(to layer: CALayer) {
            let animation = CABasicAnimation()
            animation.duration = duration
            animation.fromValue = 1
            animation.toValue = 0
            animation.delegate = self
            animation.isRemovedOnCompletion = true
            layer.add(animation, forKey: "birthRate")
        }
     
        addBirthrateAnimation(to: confettiLayer)
        view.layer.addSublayer(confettiLayer)
    }
}


extension UIViewController: CAAnimationDelegate {
    public func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        if let _ = anim.value(forKey: "completionBlock") as? (() -> Void) {
            view.layer.sublayers?.filter { $0 is CAEmitterLayer }.forEach { $0.removeFromSuperlayer() }
        }
    }
}
