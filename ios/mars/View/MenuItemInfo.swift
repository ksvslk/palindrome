//
//  UserGroupMemberPresentable.swift
//  PanModal
//
//  Created by Tosin Afolabi on 2/26/19.
//  Copyright © 2019 PanModal. All rights reserved.
//

import UIKit

struct MenuItemInfo: Equatable {
 
    let name: String
    let comment: String
    let systemImage: UIImage
    var closure: (_ view: UIView) -> Void
    
    static func == (lhs: MenuItemInfo, rhs: MenuItemInfo) -> Bool {
        return lhs.name == rhs.name
    }
}
